package cn.yzfy.crushcupidserver.logic;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.Crush;
import cn.yzfy.crushcupidserver.model.entity.CrushReport;
import cn.yzfy.crushcupidserver.model.vo.CrushReportVO;
import cn.yzfy.crushcupidserver.model.vo.SkillCatalogVO;
import cn.yzfy.crushcupidserver.model.vo.SkillMetaVO;
import cn.yzfy.crushcupidserver.security.OwnershipGuard;
import cn.yzfy.crushcupidserver.service.CrushReportService;
import cn.yzfy.crushcupidserver.skill.SkillAdvisorService;
import cn.yzfy.crushcupidserver.skill.SkillCatalogService;
import cn.yzfy.crushcupidserver.skill.SkillMeta;
import cn.yzfy.crushcupidserver.skill.SkillReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Skill 目录 / 军师模式 / 关系报告 业务逻辑层。
 * 数据访问委托 MP 薄 Service；远程模板拉取与 LLM 生成在 skill 包 Service，本层负责编排与 VO 组装。
 */
@Service
@RequiredArgsConstructor
public class SkillLogic {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SkillCatalogService skillCatalogService;
    private final SkillAdvisorService skillAdvisorService;
    private final SkillReportService skillReportService;
    private final CrushReportService crushReportService;
    private final OwnershipGuard ownershipGuard;

    /** 报告下载产物：docx 字节流 + 文件名（Controller 负责 HTTP 响应头封装） */
    public record ReportDoc(byte[] bytes, String filename) {
    }

    public SkillCatalogVO catalog() {
        SkillMeta meta = skillCatalogService.getSkillMeta();
        SkillMetaVO metaVO = new SkillMetaVO();
        BeanUtils.copyProperties(meta, metaVO);

        SkillCatalogVO vo = new SkillCatalogVO();
        vo.setSkill(metaVO);
        vo.setPrompts(skillCatalogService.listPrompts());
        return vo;
    }

    public String prompt(String name) {
        return skillCatalogService.getPrompt(name);
    }

    public List<SkillAdvisorService.AdvisorDescriptor> advisorCommands() {
        return skillAdvisorService.listDescriptors();
    }

    public String invokeAdvisor(Map<String, String> body) {
        String name = body.get("name");
        String question = body.get("question");
        String crushSlug = body.get("crushSlug");

        SkillAdvisorService.AdvisorDescriptor desc = skillAdvisorService.getDescriptor(name);
        if (desc != null && desc.requiresCrush()) {
            if (crushSlug == null || crushSlug.isBlank()) {
                throw BizException.badRequest("请先选择要分析的暗恋对象");
            }
            // 归属校验：仅本人 crush 可用于子命令/报告
            Crush crush = ownershipGuard.requireReadBySlug(crushSlug);
            if ("report".equals(desc.name())) {
                return skillReportService.generate(crushSlug);
            }
            // mirror / confess / date / progress / let-go 等模拟类子命令：注入 Persona + 关系记忆作为上下文
            return skillAdvisorService.invoke(name, question, crushContext(crush));
        }
        return skillAdvisorService.invoke(name, question, null);
    }

    public CrushReportVO generateReport(Map<String, String> body) {
        String crushSlug = body.get("crushSlug");
        if (crushSlug == null || crushSlug.isBlank()) {
            throw BizException.badRequest("缺少 crushSlug");
        }
        ownershipGuard.requireReadBySlug(crushSlug);
        CrushReport report = skillReportService.generateAndSave(crushSlug, "manual");
        return toVO(report, true);
    }

    public List<CrushReportVO> listReports(String crushSlug) {
        Crush crush = ownershipGuard.requireReadBySlug(crushSlug);
        return crushReportService.listByCrushId(crush.getId())
                .stream().map(r -> toVO(r, false)).toList();
    }

    public CrushReportVO reportDetail(Long id) {
        CrushReport report = crushReportService.getById(id);
        if (report == null) {
            throw BizException.notFound("报告不存在：" + id);
        }
        return toVO(report, true);
    }

    public void deleteReport(Long id) {
        crushReportService.removeById(id);
    }

    /** 下载某条已保存报告 .docx（读库中的 markdown，不再调用 LLM） */
    public ReportDoc downloadSaved(Long id) {
        CrushReport report = crushReportService.getById(id);
        if (report == null) {
            throw BizException.notFound("报告不存在：" + id);
        }
        byte[] bytes = skillReportService.toDocx(report.getMarkdown());
        String date = report.getReportDate() == null ? "" : report.getReportDate().toString();
        String filename = "关系报告_" + escapeFilename(report.getCrushName() == null ? "" : report.getCrushName())
                + (date.isBlank() ? "" : "_" + date) + ".docx";
        return new ReportDoc(bytes, filename);
    }

    /**
     * 下载关系报告 .docx。
     * crushSlug 必填；可选 md 参数直接使用已生成的 Markdown（避免重复调用 LLM），否则现场生成。
     */
    public ReportDoc downloadReport(String crushSlug, String md) {
        String markdown = (md == null || md.isBlank()) ? skillReportService.generate(crushSlug) : md;
        byte[] bytes = skillReportService.toDocx(markdown);
        String filename = "关系报告_" + escapeFilename(crushSlug) + "_"
                + LocalDate.now().format(DATE_FMT) + ".docx";
        return new ReportDoc(bytes, filename);
    }

    private CrushReportVO toVO(CrushReport report, boolean withMarkdown) {
        CrushReportVO vo = new CrushReportVO();
        BeanUtils.copyProperties(report, vo);
        if (!withMarkdown) {
            vo.setMarkdown(null);
        }
        return vo;
    }

    /**
     * 组装子命令所需的 crush 上下文（Persona 5 层 + 关系记忆 + 基础信息），
     * 代替参考实现中「读取 crushes/{slug}/persona.md」的本地文件动作。
     */
    private String crushContext(Crush c) {
        StringBuilder sb = new StringBuilder();
        sb.append("【基础信息】花名：").append(StrUtil.blankToDefault(c.getName(), "未知"))
                .append("；认识时长：").append(StrUtil.blankToDefault(c.getKnowDuration(), "未知"))
                .append("；关系状态：").append(StrUtil.blankToDefault(c.getRelationshipStatus(), "未知"))
                .append("；当前阶段：").append(c.getCurrentStage() == null ? 1 : c.getCurrentStage())
                .append("；MBTI：").append(StrUtil.blankToDefault(c.getMbti(), "未知"))
                .append("；星座：").append(StrUtil.blankToDefault(c.getZodiac(), "未知"))
                .append("；职业：").append(StrUtil.blankToDefault(c.getOccupation(), "未知"))
                .append("；我的印象：").append(StrUtil.blankToDefault(c.getImpression(), "无"))
                .append('\n');
        appendSection(sb, "Persona Layer0 硬规则", c.getPersonaLayer0());
        appendSection(sb, "Persona Layer1 身份", c.getPersonaLayer1());
        appendSection(sb, "Persona Layer2 说话风格", c.getPersonaLayer2());
        appendSection(sb, "Persona Layer3 情感模式", c.getPersonaLayer3());
        appendSection(sb, "Persona Layer4 关系行为", c.getPersonaLayer4());
        appendSection(sb, "关系记忆-总览", c.getMemoryOverview());
        appendSection(sb, "关系记忆-时间线", c.getMemoryTimeline());
        appendSection(sb, "关系记忆-甜蜜时刻", c.getMemorySweet());
        appendSection(sb, "关系记忆-互动模式", c.getMemoryInteraction());
        return sb.toString();
    }

    private void appendSection(StringBuilder sb, String title, String content) {
        if (StrUtil.isBlank(content)) {
            return;
        }
        sb.append("\n【").append(title).append("】\n").append(content.trim()).append('\n');
    }

    private String escapeFilename(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}