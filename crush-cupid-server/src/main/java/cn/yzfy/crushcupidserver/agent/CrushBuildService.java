package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.ChatModelRegistry;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.ChatSource;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.entity.CrushVersion;
import cn.yzfy.crushcupidserver.service.ChatSourceService;
import cn.yzfy.crushcupidserver.service.CrushService;
import cn.yzfy.crushcupidserver.service.CrushVersionService;
import cn.yzfy.crushcupidserver.model.vo.BuildResultVO;
import cn.yzfy.crushcupidserver.skill.SkillCatalogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * crush 构建流水线：读原材料 → 拉远端模板 → LLM 生成 persona/memory → 更新 + 版本快照。
 * <p>
 * 创建草稿后首次 build 生成；以后追加新材料再 build 即增量进化（复用同一套逻辑）。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CrushBuildService {

    private static final String SYSTEM_TEMPLATE = """
            你是一个「暗恋对象建模」专家。根据用户提供的基础信息和聊天记录等原材料，还原出一个像 ta 的 AI。

            必须只输出一个 JSON 对象（不要 markdown 代码块、不要任何解释），结构如下：
            {
              "personaSummary": "一句话概括 ta 的性格",
              "memorySummary": "一句话概括你们的关系",
              "persona": {
                "layer0": "硬规则（不可违背的底线）",
                "layer1": "身份（年龄/职业/MBTI/星座/与用户关系）",
                "layer2": "说话风格（口头禅/语气词/标点/emoji）",
                "layer3": "情感模式（依恋类型/表达好感/生气/吃醋）",
                "layer4": "关系行为（互动模式/回复速度/边界底线）"
              },
              "memory": {
                "overview": "关系记忆总览",
                "sweet": "甜蜜瞬间/共同经历",
                "interaction": "互动模式/约定俗成"
              }
            }

            参考以下两个模板的维度（保持 5 层 Persona + 3 维 Memory 结构）：

            ===== persona 模板 =====
            {{persona}}
            ===== memory 模板 =====
            {{memory}}
            """;

    private final CrushService crushService;
    private final ChatSourceService chatSourceService;
    private final CrushVersionService crushVersionService;
    private final SkillCatalogService skillCatalogService;
    private final ChatModelRegistry chatModelRegistry;
    private final ObjectMapper objectMapper;

    public BuildResultVO build(Long crushId, Consumer<String> onProgress) {
        Crush crush = crushService.getById(crushId);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象 id=" + crushId);
        }

        // 1. 读原材料
        List<ChatSource> sources = chatSourceService.listByCrushId(crushId);
        String rawMaterial = sources.stream()
                .map(ChatSource::getContent)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining("\n\n"));
        if (StrUtil.isBlank(rawMaterial)) {
            throw BizException.badRequest("还没有原材料，请先导入聊天记录/文件");
        }

        // 2. 存档当前版本
        snapshot(crush);

        // 3. 拉远端模板
        onProgress.accept("正在从 GitHub 拉取分析模板...");
        String personaTemplate = skillCatalogService.getPrompt("persona_builder");
        String memoryTemplate = skillCatalogService.getPrompt("memory_builder");

        // 4. LLM 生成
        onProgress.accept("正在分析原材料并生成 Persona / Memory ...");
        String json = generate(crush, rawMaterial, personaTemplate, memoryTemplate);

        // 5. 应用结果
        BuildResultVO result = apply(crush, json);
        onProgress.accept("构建完成，已生成 v" + result.getVersion());
        return result;
    }

    private void snapshot(Crush crush) {
        CrushVersion v = new CrushVersion();
        v.setCrushId(crush.getId());
        v.setVersion(crush.getVersion() == null ? 1 : crush.getVersion());
        v.setReason("build");
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("personaLayer0", crush.getPersonaLayer0());
        snap.put("personaLayer1", crush.getPersonaLayer1());
        snap.put("personaLayer2", crush.getPersonaLayer2());
        snap.put("personaLayer3", crush.getPersonaLayer3());
        snap.put("personaLayer4", crush.getPersonaLayer4());
        snap.put("memoryOverview", crush.getMemoryOverview());
        snap.put("memorySweet", crush.getMemorySweet());
        snap.put("memoryInteraction", crush.getMemoryInteraction());
        try {
            v.setSnapshot(objectMapper.writeValueAsString(snap));
        } catch (Exception e) {
            v.setSnapshot("{}");
        }
        v.setCreatedAt(new Date());
        crushVersionService.save(v);
    }

    private String generate(Crush crush, String rawMaterial, String personaTemplate, String memoryTemplate) {
        String system = SYSTEM_TEMPLATE
                .replace("{{persona}}", personaTemplate)
                .replace("{{memory}}", memoryTemplate);
        String user = buildUserPrompt(crush, rawMaterial);

        // 构建走默认供应商（如 deepseek，文本任务无需多模态）
        ChatModel chatModel = chatModelRegistry.getDefault();
        var response = chatModel.call(new Prompt(List.of(new SystemMessage(system), new UserMessage(user))));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BizException("模型返回为空");
        }
        String content = response.getResult().getOutput().getText();
        if (StrUtil.isBlank(content)) {
            throw new BizException("模型返回为空");
        }
        return content;
    }

    private String buildUserPrompt(Crush crush, String rawMaterial) {
        StringBuilder sb = new StringBuilder();
        sb.append("基础信息：\n");
        sb.append("花名：").append(StrUtil.blankToDefault(crush.getName(), "未知")).append("\n");
        sb.append("MBTI：").append(StrUtil.blankToDefault(crush.getMbti(), "未知")).append("\n");
        sb.append("星座：").append(StrUtil.blankToDefault(crush.getZodiac(), "未知")).append("\n");
        sb.append("职业：").append(StrUtil.blankToDefault(crush.getOccupation(), "未知")).append("\n");
        sb.append("关系：").append(StrUtil.blankToDefault(crush.getRelationshipStatus(), "未知")).append("\n");
        sb.append("印象：").append(StrUtil.blankToDefault(crush.getImpression(), "未知")).append("\n");
        sb.append("\n原材料（聊天记录等）：\n").append(rawMaterial);
        return sb.toString();
    }

    private BuildResultVO apply(Crush crush, String json) {
        JsonNode node = parseJson(json);
        JsonNode persona = node.path("persona");
        JsonNode memory = node.path("memory");

        crush.setPersonaLayer0(text(persona, "layer0"));
        crush.setPersonaLayer1(text(persona, "layer1"));
        crush.setPersonaLayer2(text(persona, "layer2"));
        crush.setPersonaLayer3(text(persona, "layer3"));
        crush.setPersonaLayer4(text(persona, "layer4"));
        crush.setMemoryOverview(text(memory, "overview"));
        crush.setMemorySweet(text(memory, "sweet"));
        crush.setMemoryInteraction(text(memory, "interaction"));
        crush.setStatus("READY");
        int newVersion = (crush.getVersion() == null ? 1 : crush.getVersion()) + 1;
        crush.setVersion(newVersion);
        crush.setUpdatedAt(new Date());
        crushService.updateById(crush);

        BuildResultVO vo = new BuildResultVO();
        vo.setCrushId(crush.getId());
        vo.setVersion(newVersion);
        vo.setStatus("READY");
        vo.setPersonaSummary(text(node, "personaSummary"));
        vo.setMemorySummary(text(node, "memorySummary"));
        return vo;
    }

    private JsonNode parseJson(String json) {
        String extracted = extractJson(json);
        try {
            return objectMapper.readTree(extracted);
        } catch (Exception e) {
            log.warn("解析模型输出失败：{}", e.getMessage());
            throw new BizException("构建失败，请稍后再试");
        }
    }

    private String extractJson(String text) {
        String t = text.trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return t;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? "" : v.asText();
    }
}
