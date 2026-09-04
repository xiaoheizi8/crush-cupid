package cn.yzfy.crushcupidserver.logic;

import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.CrushReport;
import cn.yzfy.crushcupidserver.model.vo.CrushReportVO;
import cn.yzfy.crushcupidserver.model.vo.SkillCatalogVO;
import cn.yzfy.crushcupidserver.model.vo.SkillMetaVO;
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
            return skillReportService.generate(crushSlug);
        }
        return skillAdvisorService.invoke(name, question, null);
    }

    public CrushReportVO generateReport(Map<String, String> body) {
        String crushSlug = body.get("crushSlug");
        if (crushSlug == null || crushSlug.isBlank()) {
            throw BizException.badRequest("缺少 crushSlug");
        }
        CrushReport report = skillReportService.generateAndSave(crushSlug, "manual");
        return toVO(report, true);
    }

    public List<CrushReportVO> listReports(String crushSlug) {
        var crush = skillReportService.lookupCrush(crushSlug);
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

    private String escapeFilename(String s) {
        return s.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}