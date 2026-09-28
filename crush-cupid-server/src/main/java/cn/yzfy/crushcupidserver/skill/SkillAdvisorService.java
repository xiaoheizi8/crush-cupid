package cn.yzfy.crushcupidserver.skill;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 军师模式（Advisor Mode）子命令注册表与调用。
 * <p>
 * 军师模式定位：模拟模式 = 「你和 ta 说话」；军师模式 = 「军师帮你分析怎么和 ta 说话」，
 * 目标是帮助用户从「依赖 AI 模拟」走向「在现实中行动」。
 * 每个子命令对应远端 skill 仓库 {@code prompts/advisor_*.md} 模板，本地维护一份触发词/说明，
 * 调用时装载模板 -> 注入变量 -> 让 LLM 以「军师」角色产出策略。
 */
@Service
@RequiredArgsConstructor
public class SkillAdvisorService {

    /** 军师固定人设（所有子命令必须保持） */
    private static final String ADVISOR_PERSONA = """
            ## 军师角色设定
            - 毒舌但靠谱，直球不绕弯，不灌鸡汤
            - 站在用户这边，但该泼冷水时绝不含糊
            - 所有建议必须具体、可执行，拒绝泛泛而谈
            - 检测到用户过度沉溺时，主动提醒回归现实
            ## 输出风格
            - 中文口语化，带点毒舌和幽默
            - 分点回答，每点附带可执行建议
            - 每次回复末尾附「军师总结」（一句话核心建议）
            ## 反例（禁止）
            - "你要相信爱情是美好的" → 灌鸡汤，零分
            - "放轻松，顺其自然就好" → 泛泛而谈，零分
            - "联系双方要认真复盘这段关系呢" → 说废话
            """;

    /** 模拟/照镜类子命令人设：贴近 persona 的真实反应，不做现实策略输出 */
    private static final String SIMULATOR_PERSONA = """
            ## 角色设定
            - 严格基于给定的 Persona（人物性格）与 Memory（关系记忆）模拟或分析，不编造原材料里没有的细节
            - 允许给出「不浪漫、不理想」的真实结果：回避、冷淡、尴尬都是合法输出
            - 结论落点是帮用户更懂这段关系，而非替代真实沟通；发现用户过度沉溺时温和提醒回归现实
            ## 输出风格
            - 中文口语化，克制毒舌、不刻意残忍
            - 按任务模板要求的结构输出
            """;

    /** 模拟/照镜类子命令集合：使用 SIMULATOR_PERSONA 而非军师人设 */
    private static final java.util.Set<String> SIMULATOR_NAMES =
            java.util.Set.of("mirror", "confess", "date", "progress", "let-go");

    /** 内置兜底模板（远端仓库暂无对应 prompts/*.md 的子命令） */
    private static final Map<String, String> BUILTIN_TEMPLATES = Map.of(
            "let_go", """
                    # Let Go — 放下（温柔的告别）

                    用户准备放下这段暗恋。请：
                    1. 用温柔的语气确认这个决定，不评判、不挽留
                    2. 简短回顾这段暗恋里最珍贵的 1-2 个瞬间（基于关系记忆，没有则用泛指）
                    3. 提醒：放下不是否认心动，而是把注意力还给自己的生活
                    4. 结尾只输出一句独立的祝福：「祝你一切都好。」
                    全文控制在 150 字以内，禁止说教。""",
            "mirror", """
                    # Mirror Mode — 照镜子模式（由 Ta 的眼睛认识自己）

                    核心：用 crush 的视角照镜子——重建「ta 眼中的你」，并模拟你和 ta 对话时的那个「你」。
                    它不模拟 ta，它模拟「ta 眼里的我」。

                    规则：
                    1. 忠实成像：用 ta 的视角，不是用户自我感觉；依据优先引用原材料中的原话/原行为
                    2. 不评判对错：只描述、不打分、不贴标签
                    3. 防两个极端：不过度自我贬低，也不自恋加工
                    4. 抓大放小：镜像重拍一次最多 3 句
                    5. 指向现实：照完镜子导向真实互动；用户反复求证而不行动时，主动点破
                    6. 素材不足时明确说「信息不够」，先问 1-2 个关键问题（如：她最近对你说过印象最深的一句话是什么），不瞎编

                    输出结构（自由咨询时按需裁剪）：
                    ## 一句话像（ta 会怎么向朋友形容你）
                    ## 高光时刻（你在 ta 眼里的加分瞬间，引用证据）
                    ## 尴尬瞬间（扣分瞬间，引用证据）
                    ## 镜像重拍（哪句话换个说法效果更好，最多 3 句）
                    ## 一句话行动建议""");

    /** 军师/模拟子命令注册表：trigger 触发词、title 标题、description 说明、promptName 远端模板、是否强制绑定 crush、icon 图标、group 分组、needsInput 是否需要用户输入、inputHint 输入提示 */
    private static final List<AdvisorDescriptor> DESCRIPTORS = List.of(
            // —— 军师模式（现实策略，对标 advisor.md 与 advisor_*.md）——
            new AdvisorDescriptor("advisor", "/advisor", "自由咨询", "开启军师对话，可自由咨询感情问题", "advisor", false,
                    "🤵", "ADVISOR", false, null),
            new AdvisorDescriptor("report", "/advisor report", "关系报告", "整合聊天记录、互动频率、信号分析，生成当前关系进展报告", "advisor_report", true,
                    "📊", "ADVISOR", false, null),
            new AdvisorDescriptor("strategy", "/advisor strategy", "策略制定", "基于当前进展阶段，推荐具体的下一步行动", "advisor_strategy", true,
                    "🧭", "ADVISOR", false, null),
            new AdvisorDescriptor("prep", "/advisor prep", "行动前准备", "约会/聊天前的战术准备：话题清单、雷区提醒、穿搭建议", "advisor_prep", false,
                    "🎒", "ADVISOR", true, "要准备什么场景？比如：周末第一次约 TA 喝咖啡"),
            new AdvisorDescriptor("analyze", "/advisor analyze", "互动复盘", "用户贴入聊天记录，军师解读对方信号", "advisor_analyze", false,
                    "💬", "ADVISOR", true, "粘贴你和 TA 的聊天记录（最好标明谁说的）"),
            new AdvisorDescriptor("confession", "/advisor confession", "告白规划", "制定告白策略：时机、方式、话术、备选方案", "advisor_confession", false,
                    "💘", "ADVISOR", false, null),
            new AdvisorDescriptor("reality", "/advisor reality", "现实检验", "客观评估暗恋健康程度，防止过度沉溺", "advisor_reality", false,
                    "🌱", "ADVISOR", false, null),
            new AdvisorDescriptor("psychology", "/analyze", "心理分析", "分析你的暗恋状态、行为模式、潜在风险和建议", "crush_analyzer", false,
                    "🧠", "ADVISOR", false, null),
            // —— 照镜子模式（对标 SKILL.md Mirror Mode：/mirror selfie|talk|gap|draft|growth，共用 mirror.md 模板）——
            new AdvisorDescriptor("mirror", "/mirror", "照镜子", "用 TA 的视角照镜子：TA 眼中的你什么样，你的话在 TA 眼里如何成像", "mirror", true,
                    "🪞", "MIRROR", false, null),
            new AdvisorDescriptor("mirror-selfie", "/mirror selfie", "镜中画像", "重建「TA 眼中的你」：TA 会向朋友怎么形容你，附证据引用", "mirror", true,
                    "🖼️", "MIRROR", false, null),
            new AdvisorDescriptor("mirror-talk", "/mirror talk", "镜像对话", "逐句看你说的话在 TA 眼里如何成像，每条附「镜像重拍」改写", "mirror", false,
                    "🗣️", "MIRROR", true, "粘贴你和 TA 的对话（标明哪句是你说的）"),
            new AdvisorDescriptor("mirror-gap", "/mirror gap", "滤镜检测", "双向镜对照「你眼中的 TA」vs「TA 眼中的你」，让理想化滤镜现形", "mirror", true,
                    "🔍", "MIRROR", false, null),
            new AdvisorDescriptor("mirror-draft", "/mirror draft", "发送预演", "还没发的草稿快速过镜：直接发 / 改一改 / 别发", "mirror", false,
                    "✏️", "MIRROR", true, "粘贴你还没发出去的草稿"),
            new AdvisorDescriptor("mirror-growth", "/mirror growth", "成长线", "结合历史报告与记忆，对比不同时期「TA 眼中的你」如何变化", "mirror", true,
                    "📈", "MIRROR", false, null),
            // —— 暗恋专属模拟器（对标 confession_simulator / date_simulator / progression_tracker / let_go）——
            new AdvisorDescriptor("confess", "/confess", "告白模拟器", "如果现在跟 TA 表白，TA 会怎么回应？模拟 3 种场景的结果与成功率", "confession_simulator", true,
                    "💝", "SIMULATOR", false, null),
            new AdvisorDescriptor("date", "/date", "约会模拟器", "模拟一次约会，预测 TA 在各种情况下的表现和反应，附约会小贴士", "date_simulator", true,
                    "🌹", "SIMULATOR", false, null),
            new AdvisorDescriptor("progress", "/progress", "进展追踪", "你现在处于暗恋的哪个阶段？记录关系进展，给出阶段建议", "progression_tracker", true,
                    "📍", "SIMULATOR", false, null),
            new AdvisorDescriptor("let-go", "/let-go", "放下", "温柔的告别：确认决定，回顾珍贵瞬间，祝你一切都好", "let_go", true,
                    "🍂", "SIMULATOR", false, null));

    /** mirror 子模式任务指令：mirror-* 命令共用 mirror.md 模板，靠这里注入具体任务语义 */
    private static final Map<String, String> MIRROR_MODE_HINTS = Map.of(
            "mirror-selfie", "画像分析：重建「TA 眼中的你」，核心是 TA 会向朋友怎么形容你，每个判断附原材料证据引用。",
            "mirror-talk", "镜像对话模拟：逐句分析用户消息在 TA 眼中的成像（成像得分无需打分，只描述），每条附「镜像重拍」改写（最多 3 句）。",
            "mirror-gap", "滤镜检测：双向镜对照「你眼中的 TA」vs「TA 眼中的你」，逐条指出理想化滤镜的偏差点。",
            "mirror-draft", "发送前预演：对用户草稿快速过镜，开头直接给结论——直接发 / 改一改（附改法）/ 别发，再用 2-3 句说明理由。",
            "mirror-growth", "成长线：结合上下文里的历史报告与关系记忆，对比不同时期「TA 眼中的你」的变化，指出成长点与需要注意的退步点。");

    private final SkillCatalogService skillCatalogService;
    private final cn.yzfy.crushcupidserver.config.ChatModelRegistry chatModelRegistry;

    /** 列出所有军师子命令描述。 */
    public List<AdvisorDescriptor> listDescriptors() {
        return DESCRIPTORS;
    }

    /**
     * 按子命令名取其描述；不存在返回 null。
     */
    public AdvisorDescriptor getDescriptor(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        return DESCRIPTORS.stream()
                .filter(d -> d.name().equalsIgnoreCase(name.trim()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 调用军师/模拟子命令，让 LLM 针对用户问题产出回复（非流式单条文本）。
     * <p>
     * 人设路由：军师类子命令用 {@link #ADVISOR_PERSONA}（现实策略），
     * 模拟/照镜类（mirror / confess / date / progress / let-go）用 {@link #SIMULATOR_PERSONA}（贴近 persona 的真实反应）。
     * <p>
     * 模板加载：优先远端 {@code prompts/{promptName}.md}，远端不存在或失败时回落 {@link #BUILTIN_TEMPLATES}。
     *
     * @param name        子命令名（advisor / report / strategy / prep / analyze / confession / reality /
     *                    mirror / mirror-selfie / mirror-talk / mirror-gap / mirror-draft / mirror-growth /
     *                    confess / date / progress / psychology / let-go）
     * @param question    用户输入/粘贴的聊天记录或问题
     * @param contextText 额外上下文（requiresCrush 子命令应传入 crush 画像/记忆摘要），可为空
     * @return LLM 产出文本
     */
    public String invoke(String name, String question, String contextText) {
        AdvisorDescriptor desc = getDescriptor(name);
        if (desc == null) {
            throw BizException.badRequest("未知的子命令：" + name + "（可选：" + joinedNames() + "）");
        }
        boolean simulator = SIMULATOR_NAMES.contains(desc.name()) || desc.name().startsWith("mirror");
        String template = loadTemplate(desc.promptName());
        Map<String, String> variables = Map.of(
                "question", StrUtil.blankToDefault(question, desc.title() + "，请给出建议"),
                "context", StrUtil.blankToDefault(contextText, "（暂无更多上下文）"));

        StringBuilder system = new StringBuilder();
        if (simulator) {
            system.append("你现在是「").append(desc.title()).append("」，不是军师，也不是暗恋对象本人。");
            system.append(SIMULATOR_PERSONA);
        } else {
            system.append("你是一名暗恋军师。");
            system.append(ADVISOR_PERSONA);
        }
        if (StrUtil.isNotBlank(template)) {
            system.append("\n\n## 本次任务模板\n").append(template);
        }
        // mirror 子模式共用 mirror.md 模板，靠这里注入具体任务语义
        String modeHint = MIRROR_MODE_HINTS.get(desc.name());
        if (modeHint != null) {
            system.append("\n\n## 本次子模式任务\n").append(modeHint);
        }

        String user = "问题/材料：\n" + variables.get("question")
                + "\n\n上下文（Persona / 关系记忆 / 基础信息，如已提供则以它为准）：\n" + variables.get("context");

        ChatModel chatModel = chatModelRegistry.getDefault();
        var response = chatModel.call(new Prompt(List.of(
                new SystemMessage(system.toString()),
                new UserMessage(user))));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BizException("模型返回为空");
        }
        String content = response.getResult().getOutput().getText();
        if (StrUtil.isBlank(content)) {
            throw new BizException("模型返回为空");
        }
        return content.trim();
    }

    /** 军师固定人设（供子命令 / 报告生成复用） */
    public String publicPersonaSnippet() {
        return ADVISOR_PERSONA;
    }

    /**
     * 组装军师模式的完整系统提示（供对话页军师开关流式注入使用）：
     * 军师人设 + 可选注入的 skill prompt 任务。返回文本由调用方塞进 system。
     */
    public String advisorSystemPrompt(String skillPrompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("你现在是「暗恋军师」，不是暗恋对象本人。");
        sb.append("目标：帮用户分析「怎么和 ta 说话、要不要行动」，而不是替用户模拟对话。\n");
        sb.append(ADVISOR_PERSONA);
        if (StrUtil.isNotBlank(skillPrompt)) {
            sb.append("\n\n## 本次任务要求\n").append(skillPrompt.trim()).append("\n");
        }
        return sb.toString();
    }

    private String loadTemplate(String promptName) {
        try {
            String t = skillCatalogService.getPrompt(promptName);
            if (StrUtil.isNotBlank(t)) {
                return t;
            }
        } catch (Exception ignored) {
            // 远端仓库不可达 / 模板不存在时回落内置模板
        }
        return BUILTIN_TEMPLATES.getOrDefault(promptName, "");
    }

    private String joinedNames() {
        return String.join(" / ", DESCRIPTORS.stream().map(AdvisorDescriptor::name).toList());
    }

    /**
     * 军师子命令描述（不可变记录）。
     * icon/group/needsInput/inputHint 随命令列表接口下发，供三端渲染分组着色卡片与输入对话框。
     */
    public record AdvisorDescriptor(String name, String trigger, String title, String description,
                                    String promptName, boolean requiresCrush,
                                    String icon, String group, boolean needsInput, String inputHint) {
    }
}
