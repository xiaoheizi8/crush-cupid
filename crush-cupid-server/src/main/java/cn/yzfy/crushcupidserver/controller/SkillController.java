package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.SkillLogic;
import cn.yzfy.crushcupidserver.model.vo.CrushReportVO;
import cn.yzfy.crushcupidserver.model.vo.SkillCatalogVO;
import cn.yzfy.crushcupidserver.skill.SkillAdvisorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Skill 目录 / 军师模式 / 关系报告（薄控制器：仅参数绑定与 HTTP 响应封装，业务逻辑在 {@link SkillLogic}）。
 */
@RestController
@RequestMapping("/api/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillLogic skillLogic;

    @GetMapping("/catalog")
    public Result<SkillCatalogVO> catalog() {
        return Result.ok(skillLogic.catalog());
    }

    @GetMapping("/prompt/{name}")
    public Result<String> prompt(@PathVariable String name) {
        return Result.ok(skillLogic.prompt(name));
    }

    /** 军师模式子命令列表（供前端渲染子命令卡片，含触发词/说明） */
    @GetMapping("/advisor")
    public Result<List<SkillAdvisorService.AdvisorDescriptor>> advisorCommands() {
        return Result.ok(skillLogic.advisorCommands());
    }

    /** 调用军师子命令，让 LLM 以军师角色回应；requiresCrush 子命令（report）需 crushSlug */
    @PostMapping("/advisor/invoke")
    public Result<String> invokeAdvisor(@RequestBody Map<String, String> body) {
        return Result.ok(skillLogic.invokeAdvisor(body));
    }

    /** 生成关系报告（Markdown）并落库，供前端预览；返回含 id 的报告详情 */
    @PostMapping("/advisor/report")
    public Result<CrushReportVO> generateReport(@RequestBody Map<String, String> body) {
        return Result.ok(skillLogic.generateReport(body));
    }

    /** 某暗恋对象的关系报告历史（列表，新→旧） */
    @GetMapping("/report/list")
    public Result<List<CrushReportVO>> listReports(@RequestParam String crushSlug) {
        return Result.ok(skillLogic.listReports(crushSlug));
    }

    /** 报告详情（含 markdown 全文） */
    @GetMapping("/report/{id}")
    public Result<CrushReportVO> reportDetail(@PathVariable Long id) {
        return Result.ok(skillLogic.reportDetail(id));
    }

    /** 删除一条报告历史 */
    @DeleteMapping("/report/{id}")
    public Result<Void> deleteReport(@PathVariable Long id) {
        skillLogic.deleteReport(id);
        return Result.ok();
    }

    /** 下载某条已保存报告 .docx */
    @GetMapping("/report/{id}/download")
    public ResponseEntity<byte[]> downloadSaved(@PathVariable Long id) {
        SkillLogic.ReportDoc doc = skillLogic.downloadSaved(id);
        return downloadResponse(doc);
    }

    /** 下载关系报告 .docx（crushSlug 必填；可选 md 直接使用已生成 Markdown，否则现场生成） */
    @GetMapping("/advisor/report/download")
    public ResponseEntity<byte[]> downloadReport(@RequestParam String crushSlug,
                                                 @RequestParam(required = false) String md) {
        return downloadResponse(skillLogic.downloadReport(crushSlug, md));
    }

    private ResponseEntity<byte[]> downloadResponse(SkillLogic.ReportDoc doc) {
        String encoded = URLEncoder.encode(doc.filename(), StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(doc.bytes().length)
                .body(doc.bytes());
    }
}