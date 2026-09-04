package cn.yzfy.crushcupidserver.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.UserProviderLogic;
import cn.yzfy.crushcupidserver.model.dto.AiProviderDTO;
import cn.yzfy.crushcupidserver.model.vo.AiProviderVO;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
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
 * 用户私有 LLM 供应商（Phase 4）。归属当前登录用户，key 加密落库、脱敏下发。
 * 路由代号为 {userId}:{providerKey}，聊天时作为 provider 传入即可命中本人私有模型。
 */
@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
public class UserProviderController {

    private final UserProviderLogic userProviderLogic;

    @GetMapping
    @SaCheckPermission("provider:self:read")
    public Result<List<AiProviderVO>> list() {
        return Result.ok(userProviderLogic.list(SecurityUtils.currentUserId()));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("provider:self:read")
    public Result<AiProviderVO> get(@PathVariable Long id) {
        return Result.ok(userProviderLogic.get(SecurityUtils.currentUserId(), id));
    }

    @PostMapping
    @SaCheckPermission("provider:self:write")
    public Result<AiProviderVO> create(@RequestBody AiProviderDTO dto) {
        return Result.ok(userProviderLogic.create(SecurityUtils.currentUserId(), dto));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("provider:self:write")
    public Result<AiProviderVO> update(@PathVariable Long id, @RequestBody AiProviderDTO dto) {
        return Result.ok(userProviderLogic.update(SecurityUtils.currentUserId(), id, dto));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("provider:self:write")
    public Result<Void> delete(@PathVariable Long id) {
        userProviderLogic.delete(SecurityUtils.currentUserId(), id);
        return Result.ok();
    }
}
