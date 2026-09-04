package cn.yzfy.crushcupidserver.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.AiProviderLogic;
import cn.yzfy.crushcupidserver.model.dto.AiProviderDTO;
import cn.yzfy.crushcupidserver.model.vo.AiProviderVO;
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
 * 自定义大模型 API 供应商管理（薄控制器：仅参数绑定，业务逻辑在 {@link AiProviderLogic}）。
 */
@RestController
@RequestMapping("/api/ai-provider")
@SaCheckRole(value = {"ROLE_ADMIN", "ROLE_OPERATOR"}, mode = SaMode.OR)
@RequiredArgsConstructor
public class AiProviderController {

    private final AiProviderLogic aiProviderLogic;

    @GetMapping
    @SaCheckPermission("admin:provider:read")
    public Result<List<AiProviderVO>> list() {
        return Result.ok(aiProviderLogic.list());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("admin:provider:read")
    public Result<AiProviderVO> get(@PathVariable Long id) {
        return Result.ok(aiProviderLogic.get(id));
    }

    @PostMapping
    @SaCheckPermission("admin:provider:write")
    public Result<AiProviderVO> create(@RequestBody AiProviderDTO dto) {
        return Result.ok(aiProviderLogic.create(dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("admin:provider:write")
    public Result<AiProviderVO> update(@PathVariable Long id, @RequestBody AiProviderDTO dto) {
        return Result.ok(aiProviderLogic.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("admin:provider:write")
    public Result<Void> delete(@PathVariable Long id) {
        aiProviderLogic.delete(id);
        return Result.ok();
    }
}