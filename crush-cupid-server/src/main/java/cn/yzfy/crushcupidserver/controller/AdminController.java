package cn.yzfy.crushcupidserver.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.yzfy.crushcupidserver.common.Result;
import cn.yzfy.crushcupidserver.logic.AdminLogic;
import cn.yzfy.crushcupidserver.model.dto.AdminResetPwdDTO;
import cn.yzfy.crushcupidserver.model.dto.AssignRoleDTO;
import cn.yzfy.crushcupidserver.model.dto.QuotaUpdateDTO;
import cn.yzfy.crushcupidserver.model.entity.SysAuditLog;
import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import cn.yzfy.crushcupidserver.model.vo.AdminUserVO;
import cn.yzfy.crushcupidserver.model.vo.OverviewVO;
import cn.yzfy.crushcupidserver.model.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台管理接口（Phase 6）。类级限制 ADMIN/OPERATOR 角色，方法级细粒度权限。
 */
@RestController
@RequestMapping("/api/admin")
@SaCheckRole(value = {"ROLE_ADMIN", "ROLE_OPERATOR"}, mode = SaMode.OR)
@RequiredArgsConstructor
public class AdminController {

    private final AdminLogic adminLogic;

    // ---- 概览 ----
    @GetMapping("/overview")
    @SaCheckPermission("admin:dashboard:read")
    public Result<OverviewVO> overview() {
        return Result.ok(adminLogic.overview());
    }

    // ---- 用户管理 ----
    @GetMapping("/users")
    @SaCheckPermission("admin:user:read")
    public Result<PageResult<AdminUserVO>> listUsers(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size,
                                                     @RequestParam(required = false) String keyword) {
        return Result.ok(adminLogic.listUsers(Math.max(page, 1), Math.min(Math.max(size, 1), 100), keyword));
    }

    @GetMapping("/user/{id}")
    @SaCheckPermission("admin:user:read")
    public Result<AdminUserVO> userDetail(@PathVariable Long id) {
        return Result.ok(adminLogic.userDetail(id));
    }

    @PostMapping("/user/{id}/enable")
    @SaCheckPermission("admin:user:write")
    public Result<AdminUserVO> enable(@PathVariable Long id) {
        return Result.ok(adminLogic.setUserStatus(id, 1));
    }

    @PostMapping("/user/{id}/disable")
    @SaCheckPermission("admin:user:write")
    public Result<AdminUserVO> disable(@PathVariable Long id) {
        return Result.ok(adminLogic.setUserStatus(id, 0));
    }

    @PostMapping("/user/{id}/reset-password")
    @SaCheckPermission("admin:user:resetPwd")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody AdminResetPwdDTO dto) {
        adminLogic.resetPassword(id, dto);
        return Result.ok();
    }

    @PostMapping("/user/{id}/role")
    @SaCheckPermission("admin:user:write")
    public Result<Void> assignRole(@PathVariable Long id, @RequestBody AssignRoleDTO dto) {
        adminLogic.assignRole(id, dto);
        return Result.ok();
    }

    @PostMapping("/user/{id}/kickout")
    @SaCheckPermission("admin:user:write")
    public Result<Void> kickout(@PathVariable Long id) {
        adminLogic.kickout(id);
        return Result.ok();
    }

    // ---- 配额 ----
    @GetMapping("/quota/{id}")
    @SaCheckPermission("admin:quota:read")
    public Result<SysQuota> getQuota(@PathVariable Long id) {
        return Result.ok(adminLogic.getQuota(id));
    }

    @PutMapping("/quota/{id}")
    @SaCheckPermission("admin:quota:write")
    public Result<SysQuota> updateQuota(@PathVariable Long id, @RequestBody QuotaUpdateDTO dto) {
        return Result.ok(adminLogic.updateQuota(id, dto));
    }

    // ---- 审计 ----
    @GetMapping("/audit")
    @SaCheckPermission("admin:audit:read")
    public Result<PageResult<SysAuditLog>> listAudit(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "20") long size,
                                                     @RequestParam(required = false) String module,
                                                     @RequestParam(required = false) String email,
                                                     @RequestParam(required = false) String result) {
        return Result.ok(adminLogic.listAudit(Math.max(page, 1), Math.min(Math.max(size, 1), 200), module, email, result));
    }

    // ---- 配置 ----
    @GetMapping("/config")
    @SaCheckPermission("admin:config:read")
    public Result<Map<String, String>> listConfig() {
        return Result.ok(adminLogic.listConfig());
    }

    @PutMapping("/config/{key}")
    @SaCheckPermission("admin:config:write")
    public Result<Void> updateConfig(@PathVariable String key, @RequestBody String value) {
        adminLogic.updateConfig(key, value);
        return Result.ok();
    }
}
