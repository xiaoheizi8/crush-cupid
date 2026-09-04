package cn.yzfy.crushcupidserver.controller;

import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.CrushLogic;
import cn.yzfy.crushcupidserver.model.dto.CrushCreateDTO;
import cn.yzfy.crushcupidserver.model.dto.CrushUpdateDTO;
import cn.yzfy.crushcupidserver.model.vo.CrushVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 暗恋对象 CRUD（薄控制器：仅参数绑定，业务逻辑在 {@link CrushLogic}）。
 */
@RestController
@RequestMapping("/api/crush")
@RequiredArgsConstructor
public class CrushController {

    private final CrushLogic crushLogic;

    @GetMapping
    public Result<List<CrushVO>> list() {
        return Result.ok(crushLogic.list());
    }

    @GetMapping("/{id}")
    public Result<CrushVO> get(@PathVariable Long id) {
        return Result.ok(crushLogic.get(id));
    }

    @PostMapping
    public Result<CrushVO> create(@RequestBody CrushCreateDTO dto) {
        return Result.ok(crushLogic.create(dto));
    }

    @PutMapping("/{id}")
    public Result<CrushVO> update(@PathVariable Long id, @RequestBody CrushUpdateDTO dto) {
        return Result.ok(crushLogic.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        crushLogic.delete(id);
        return Result.ok();
    }
}