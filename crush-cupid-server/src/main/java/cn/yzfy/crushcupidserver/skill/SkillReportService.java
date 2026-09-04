package cn.yzfy.crushcupidserver.skill;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.Conversation;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.entity.CrushReport;
import cn.yzfy.crushcupidserver.service.ConversationService;
import cn.yzfy.crushcupidserver.service.CrushReportService;
import cn.yzfy.crushcupidserver.service.CrushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 关系报告生成与文档导出。
 * <p>
 * 生成链路：装载远端 {@code advisor_report.md} 模板 -> 汇总 crush 画像/记忆/最近对话 ->
 * 让 LLM 以「军师」角色产出结构化 Markdown 关系报告 -> 用 Apache POI 渲染为 .docx 供下载。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillReportService {

    private static final int RECENT_CONV_LIMIT = 200;
    /** 清洗对话文本里的图片标记，避免 LLM 看到占位符/内联残留 */
    private static final Pattern MEDIA_MARKER = Pattern.compile("\\[\\[图片:[^\\]]*\\]\\]|\\[图片\\]");

    private final CrushService crushService;
    private final ConversationService conversationService;
    private final CrushReportService crushReportService;
    private final SkillCatalogService skillCatalogService;
    private final cn.yzfy.crushcupidserver.config.ChatModelRegistry chatModelRegistry;
    private final SkillAdvisorService skillAdvisorService;
    private final cn.yzfy.crushcupidserver.security.UserChatCipher userChatCipher;

    /**
     * 生成关系报告（Markdown 文本）。
     *
     * @param crushSlug 暗恋对象 slug
     * @return 报告 Markdown
     */
    public String generate(String crushSlug) {
        Crush crush = crushService.getBySlug(crushSlug);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象：" + crushSlug);
        }
        String raw = rawConversation(crush.getId(), crush.getUserId() == null ? 0L : crush.getUserId());
        String profile = buildProfile(crush);
        String template = loadReportTemplate();

        String system = "你是一名暗恋军师，负责为用户生成「关系进展报告」。\n"
                + skillAdvisorService.publicPersonaSnippet()
                + "\n\n## 报告模板\n" + template;

        String user = "暗恋对象画像：\n" + profile
                + "\n\n最近对话记录（越大越新）：\n" + (StrUtil.isBlank(raw) ? "（暂无对话记录）" : raw);

        ChatModel chatModel = chatModelRegistry.getDefault();
        var response = chatModel.call(new Prompt(List.of(
                new SystemMessage(system),
                new UserMessage(user))));
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new BizException("模型返回为空");
        }
        String content = response.getResult().getOutput().getText();
        if (StrUtil.isBlank(content)) {
            throw new BizException("模型返回为空");
        }
        String md = content.trim();
        return "# 关系进展报告：" + crush.getName() + "\n\n"
                + "生成时间：" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + "\n\n"
                + stripLeadingH1(md);
    }

    /**
     * 生成关系报告并落库返回实体。
     *
     * @param crushSlug 暗恋对象 slug
     * @param source    生成来源：manual / scheduled
     * @return 已持久化的 {@link CrushReport}
     */
    public CrushReport generateAndSave(String crushSlug, String source) {
        Crush crush = crushService.getBySlug(crushSlug);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象：" + crushSlug);
        }
        String md = generate(crushSlug);
        CrushReport report = new CrushReport();
        report.setCrushId(crush.getId());
        report.setCrushName(crush.getName());
        report.setTitle("关系进展报告：" + crush.getName());
        report.setMarkdown(md);
        report.setSource(StrUtil.isBlank(source) ? "manual" : source);
        report.setReportDate(java.sql.Date.valueOf(LocalDate.now()));
        report.setCreatedAt(new java.util.Date());
        crushReportService.save(report);
        return report;
    }

    /**
     * 把 Markdown 报告导出为 .docx 字节。
     */
    public byte[] toDocx(String markdown) {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            appendMarkdown(doc, markdown == null ? "" : markdown);
            doc.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BizException("生成 Word 文档失败：" + e.getMessage());
        }
    }

    /** 按 slug 取暗恋对象（不存在抛 404），供 Controller 复用 */
    public Crush lookupCrush(String crushSlug) {
        Crush crush = crushService.getBySlug(crushSlug);
        if (crush == null) {
            throw BizException.notFound("未找到暗恋对象：" + crushSlug);
        }
        return crush;
    }

    private String loadReportTemplate() {
        try {
            String t = skillCatalogService.getPrompt("advisor_report");
            return t == null ? "" : t;
        } catch (Exception e) {
            return "";
        }
    }

    /** 汇总 crush 画像/记忆为报告上下文 */
    private String buildProfile(Crush c) {
        StringBuilder sb = new StringBuilder();
        sb.append("花名：").append(StrUtil.blankToDefault(c.getName(), "未知"))
                .append("\n关系：").append(StrUtil.blankToDefault(c.getRelationshipStatus(), "未知"))
                .append("\n认识时长：").append(StrUtil.blankToDefault(c.getKnowDuration(), "未知"))
                .append("\nMBTI：").append(StrUtil.blankToDefault(c.getMbti(), "未知"))
                .append("\n星座：").append(StrUtil.blankToDefault(c.getZodiac(), "未知"))
                .append("\n职业：").append(StrUtil.blankToDefault(c.getOccupation(), "未知"))
                .append("\n印象：").append(StrUtil.blankToDefault(c.getImpression(), "未知"));
        appendIfNotBlank(sb, "关系记忆", c.getMemoryOverview());
        appendIfNotBlank(sb, "时间线", c.getMemoryTimeline());
        appendIfNotBlank(sb, "甜蜜瞬间", c.getMemorySweet());
        appendIfNotBlank(sb, "互动模式", c.getMemoryInteraction());
        return sb.toString();
    }

    private void appendIfNotBlank(StringBuilder sb, String title, String content) {
        if (StrUtil.isNotBlank(content)) {
            sb.append("\n## ").append(title).append("\n").append(content);
        }
    }

    /** 取最近对话（以归属用户的真实消息为主，清洗图片标记，限制条数） */
    private String rawConversation(Long crushId, long ownerId) {
        List<Conversation> rows = conversationService.lambdaQuery()
                .eq(Conversation::getCrushId, crushId)
                .eq(Conversation::getUserId, ownerId)
                .orderByDesc(Conversation::getCreatedAt)
                .last("limit " + RECENT_CONV_LIMIT)
                .list();
        // 倒序恢复时间正序
        java.util.Collections.reverse(rows);
        return rows.stream()
                .filter(r -> StrUtil.isNotBlank(userChatCipher.decryptForUser(r.getContent(), ownerId)))
                .map(r -> "[" + r.getRole() + "] " + cleanMedia(userChatCipher.decryptForUser(r.getContent(), ownerId)))
                .collect(Collectors.joining("\n"));
    }

    private String cleanMedia(String s) {
        return MEDIA_MARKER.matcher(s).replaceAll("").trim();
    }

    private String stripLeadingH1(String md) {
        String t = md.trim();
        return t.replaceAll("^#\\s.*?(\n|$)", "");
    }

    /** 把简单 Markdown 段落/标题/列表渲染进 docx */
    private void appendMarkdown(XWPFDocument doc, String md) {
        String[] lines = md.split("\r?\n");
        for (String line : lines) {
            String t = line.strip();
            if (t.isBlank()) {
                continue;
            }
            if (t.startsWith("#")) {
                int level = 0;
                while (level < t.length() && t.charAt(level) == '#') {
                    level++;
                }
                addHeading(doc, t.substring(level).strip(), Math.min(level, 3));
            } else if (t.startsWith("- ") || t.startsWith("* ")) {
                addRun(doc, "• " + t.substring(2).strip(), false, 11, false);
            } else if (t.matches("\\d+\\.\\s+.*")) {
                addRun(doc, t, false, 11, false);
            } else {
                addRun(doc, t, false, 11, false);
            }
        }
    }

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(120);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(level == 1 ? 18 : (level == 2 ? 15 : 12));
        run.setFontFamily("微软雅黑");
    }

    private void addRun(XWPFDocument doc, String text, boolean bold, int size, boolean addSpacing) {
        XWPFParagraph p = doc.createParagraph();
        if (addSpacing) {
            p.setSpacingAfter(100);
        }
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(size);
        run.setFontFamily("微软雅黑");
    }
}
