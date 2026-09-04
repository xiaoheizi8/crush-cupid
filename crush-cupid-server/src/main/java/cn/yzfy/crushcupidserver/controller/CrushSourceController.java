package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.CrushSourceLogic;
import cn.yzfy.crushcupidserver.model.dto.SourceImportDTO;
import cn.yzfy.crushcupidserver.model.vo.SourceVO;
import cn.yzfy.crushcupidserver.model.vo.VersionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * crush 生命周期子资源（薄控制器：仅参数绑定，业务逻辑在 {@link CrushSourceLogic}）。
 */
@RestController
@RequestMapping("/api/crush/{crushId}")
@RequiredArgsConstructor
public class CrushSourceController {

    private final CrushSourceLogic crushSourceLogic;

    @PostMapping("/sources")
    public Result<SourceVO> importSource(@PathVariable Long crushId, @RequestBody SourceImportDTO dto) {
        return Result.ok(crushSourceLogic.importSource(crushId, dto));
    }

    @PostMapping("/sources/upload")
    public Result<SourceVO> uploadSource(@PathVariable Long crushId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "type", required = false) String type) {
        return Result.ok(crushSourceLogic.uploadSource(crushId, file, type));
    }

    @GetMapping("/sources")
    public Result<List<SourceVO>> listSources(@PathVariable Long crushId) {
        return Result.ok(crushSourceLogic.listSources(crushId));
    }

    @DeleteMapping("/sources/{sourceId}")
    public Result<Void> deleteSource(@PathVariable Long crushId, @PathVariable Long sourceId) {
        crushSourceLogic.deleteSource(crushId, sourceId);
        return Result.ok();
    }

    @PostMapping(value = "/build", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter build(@PathVariable Long crushId) {
        return crushSourceLogic.build(crushId);
    }

    @GetMapping("/versions")
    public Result<List<VersionVO>> listVersions(@PathVariable Long crushId) {
        return Result.ok(crushSourceLogic.listVersions(crushId));
    }
}