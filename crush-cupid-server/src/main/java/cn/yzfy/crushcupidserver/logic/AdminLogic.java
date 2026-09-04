package cn.yzfy.crushcupidserver.logic;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.dto.AdminResetPwdDTO;
import cn.yzfy.crushcupidserver.model.dto.AssignRoleDTO;
import cn.yzfy.crushcupidserver.model.dto.QuotaUpdateDTO;
import cn.yzfy.crushcupidserver.model.entity.SysAuditLog;
import cn.yzfy.crushcupidserver.model.entity.SysConfig;
import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.model.vo.AdminUserVO;
import cn.yzfy.crushcupidserver.model.vo.OverviewVO;
import cn.yzfy.crushcupidserver.model.vo.PageResult;
import cn.yzfy.crushcupidserver.security.CryptoHelper;
import cn.yzfy.crushcupidserver.security.SecurityUtils;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import cn.yzfy.crushcupidserver.service.CrushService;
import cn.yzfy.crushcupidserver.service.SysAuditLogService;
import cn.yzfy.crushcupidserver.service.SysConfigService;
import cn.yzfy.crushcupidserver.service.SysQuotaService;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import cn.yzfy.crushcupidserver.service.SysUsageDailyService;
import cn.yzfy.crushcupidserver.service.SysUserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 后台管理业务逻辑（Phase 6）：用户管理、配额、审计、配置、概览。
 * 权限在 Controller 用 @SaCheckRole/@SaCheckPerm 控制；敏感操作（分配 ADMIN/重置密码）此处二次把关。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLogic {

    private static final Pattern PASSWORD_REGEX =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");

    private final SysUserService sysUserService;
    private final SysRbacService sysRbacService;
    private final SysQuotaService sysQuotaService;
    private final QuotaLogic quotaLogic;
    private final SysAuditLogService sysAuditLogService;
    private final SysConfigService sysConfigService;
    private final CrushService crushService;
    private final SysUsageDailyService sysUsageDailyService;
    private final AiProviderService aiProviderService;
    private final CryptoHelper cryptoHelper;
    private final ObjectMapper objectMapper;

    // ---------------- 用户管理 ----------------

    public PageResult<AdminUserVO> listUsers(long page, long size, String keyword) {
        var p = sysUserService.pageUsers(page, size, keyword);
        List<AdminUserVO> records = p.getRecords().stream().map(this::toAdminUser).toList();
        return new PageResult<>(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    public AdminUserVO userDetail(Long userId) {
        SysUser u = sysUserService.getById(userId);
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        return toAdminUser(u);
    }

    public AdminUserVO setUserStatus(Long userId, int status) {
        SysUser u = requireUser(userId);
        u.setStatus(status);
        u.setUpdatedAt(new Date());
        sysUserService.updateById(u);
        auditSelf("admin", "UPDATE", "User", String.valueOf(userId),
                Map.of("status", status), 0);
        return toAdminUser(u);
    }

    public void resetPassword(Long userId, AdminResetPwdDTO dto) {
        requireAdminOperator();
        SysUser u = requireUser(userId);
        if (dto.getNewPassword() == null || !PASSWORD_REGEX.matcher(dto.getNewPassword()).matches()) {
            throw BizException.badRequest("密码需 8~64 位，且包含大小写字母和数字");
        }
        u.setPasswordHash(cryptoHelper.encodePassword(dto.getNewPassword()));
        u.setFailedAttempt(0);
        u.setLockedUntil(null);
        u.setUpdatedAt(new Date());
        sysUserService.updateById(u);
        // 重置密码后踢下线，强制重新登录
        try {
            StpUtil.kickout(userId);
        } catch (Exception e) {
            log.warn("踢下线失败 userId={}: {}", userId, e.getMessage());
        }
        auditSelf("admin", "RESET_PWD", "User", String.valueOf(userId), null, 0);
    }

    public void assignRole(Long userId, AssignRoleDTO dto) {
        requireAdminOperator();
        if (dto.getRoleCode() == null || dto.getRoleCode().isBlank()) {
            throw BizException.badRequest("roleCode 不能为空");
        }
        // 分配 ADMIN 角色：仅真正 ADMIN 可操作（OPERATOR 无此权限，见权限表）
        if (Objects.equals(dto.getRoleCode(), "ADMIN") || Objects.equals(dto.getRoleCode(), "ROLE_ADMIN")) {
            long me = SecurityUtils.currentUserId();
            if (!sysRbacService.isAdmin(me)) {
                throw BizException.forbidden("仅 ADMIN 可分配管理员角色");
            }
        }
        requireUser(userId);
        sysRbacService.assignRole(userId, dto.getRoleCode());
        auditSelf("admin", "ASSIGN_ROLE", "User", String.valueOf(userId),
                Map.of("role", dto.getRoleCode()), 0);
    }

    public void kickout(Long userId) {
        requireUser(userId);
        StpUtil.kickout(userId);
        auditSelf("admin", "KICKOUT", "User", String.valueOf(userId), null, 0);
    }

    // ---------------- 配额 ----------------

    public SysQuota getQuota(Long userId) {
        requireUser(userId);
        return quotaLogic.getOrCreateQuota(userId);
    }

    public SysQuota updateQuota(Long userId, QuotaUpdateDTO dto) {
        requireUser(userId);
        SysQuota quota = quotaLogic.getOrCreateQuota(userId);
        if (dto.getPlan() != null) quota.setPlan(dto.getPlan());
        if (dto.getCrushLimit() != null) quota.setCrushLimit(Math.max(1, dto.getCrushLimit()));
        if (dto.getDailyChatLimit() != null) quota.setDailyChatLimit(Math.max(1, dto.getDailyChatLimit()));
        if (dto.getModelAllowlist() != null) {
            quota.setModelAllowlist(toJson(dto.getModelAllowlist()));
        }
        quota.setUpdatedAt(new Date());
        sysQuotaService.updateById(quota);
        auditSelf("admin", "UPDATE", "Quota", String.valueOf(userId), null, 0);
        return quota;
    }

    // ---------------- 审计 ----------------

    public PageResult<SysAuditLog> listAudit(long page, long size, String module, String email, String result) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<SysAuditLog>()
                .orderByDesc(SysAuditLog::getCreatedAt);
        if (StrUtil.isNotBlank(module)) wrapper.eq(SysAuditLog::getModule, module);
        if (StrUtil.isNotBlank(email)) wrapper.like(SysAuditLog::getEmail, email);
        if (StrUtil.isNotBlank(result)) wrapper.eq(SysAuditLog::getResult, result);
        var p = sysAuditLogService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal(), p.getCurrent(), p.getSize());
    }

    // ---------------- 配置 ----------------

    public Map<String, String> listConfig() {
        return sysConfigService.list().stream()
                .collect(java.util.stream.Collectors.toMap(SysConfig::getConfigKey,
                        c -> c.getConfigValue() == null ? "" : c.getConfigValue()));
    }

    public void updateConfig(String key, String value) {
        SysConfig cfg = sysConfigService.getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key).last("LIMIT 1"));
        if (cfg == null) {
            cfg = new SysConfig();
            cfg.setConfigKey(key);
        }
        cfg.setConfigValue(value);
        cfg.setUpdatedBy(SecurityUtils.currentUserId());
        cfg.setUpdatedAt(new Date());
        sysConfigService.saveOrUpdate(cfg);
        auditSelf("admin", "UPDATE", "Config", key, null, 0);
    }

    // ---------------- 概览 ----------------

    public OverviewVO overview() {
        OverviewVO vo = new OverviewVO();
        vo.setUserCount(sysUserService.count());
        vo.setCrushCount(crushService.count());
        vo.setProviderCount(aiProviderService.count());
        vo.setTodayChats(countTodayUsageRows());
        return vo;
    }

    private int countTodayUsageRows() {
        try {
            return Math.toIntExact(sysUsageDailyService.count(
                    new LambdaQueryWrapper<cn.yzfy.crushcupidserver.model.entity.SysUsageDaily>()
                            .eq(cn.yzfy.crushcupidserver.model.entity.SysUsageDaily::getUsageDate, LocalDate.now())));
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------- 内部 ----------------

    private SysUser requireUser(Long userId) {
        SysUser u = sysUserService.getById(userId);
        if (u == null) {
            throw BizException.notFound("用户不存在");
        }
        return u;
    }

    private void requireAdminOperator() {
        long me = SecurityUtils.currentUserId();
        if (!sysRbacService.isAdmin(me)) {
            throw BizException.forbidden("无此权限");
        }
    }

    private AdminUserVO toAdminUser(SysUser u) {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(u.getId());
        vo.setEmail(u.getEmail());
        vo.setUsername(u.getUsername());
        vo.setStatus(u.getStatus());
        vo.setEmailVerified(u.getEmailVerified());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setLastLoginAt(u.getLastLoginAt());
        vo.setRoles(sysRbacService.getRoleCodes(u.getId()));
        SysQuota q = sysQuotaService.getByUserId(u.getId());
        vo.setPlan(q == null ? "free" : q.getPlan());
        return vo;
    }

    private String toJson(Object o) {
        if (o == null) return null;
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return null;
        }
    }

    private void auditSelf(String module, String action, String rt, String rid, Map<String, Object> detail, long ms) {
        // 注意：AdminLogic 直接写审计，避免调用 AuditLogic 出现循环依赖（AuditLogic 依赖 SysUserService）
        cn.yzfy.crushcupidserver.model.entity.SysAuditLog entry = new cn.yzfy.crushcupidserver.model.entity.SysAuditLog();
        entry.setModule(module);
        entry.setAction(action);
        entry.setResourceType(rt);
        entry.setResourceId(rid);
        entry.setResult("SUCCESS");
        entry.setDetail(toJson(detail));
        entry.setLatencyMs((int) ms);
        entry.setCreatedAt(new Date());
        try {
            if (StpUtil.isLogin()) {
                Long me = StpUtil.getLoginIdAsLong();
                entry.setUserId(me);
                SysUser m = sysUserService.getById(me);
                if (m != null) entry.setEmail(m.getEmail());
            }
            sysAuditLogService.save(entry);
        } catch (Exception e) {
            log.warn("admin 审计写入失败: {}", e.getMessage());
        }
    }
}
