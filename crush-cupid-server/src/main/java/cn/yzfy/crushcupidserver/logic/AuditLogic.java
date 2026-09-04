package cn.yzfy.crushcupidserver.logic;

import cn.dev33.satoken.stp.StpUtil;
import cn.yzfy.crushcupidserver.model.entity.SysAuditLog;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.service.SysAuditLogService;
import cn.yzfy.crushcupidserver.service.SysUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * 审计埋点：who/when/what/result/ip/ua 写 sys_audit_log（只增不改）。
 * <p>
 * 约束：detail 只记摘要（id/计数/枚举），严禁聊天明文、供应商 Key 明文（安全红线）。
 * 写入失败绝不向上抛出，避免审计故障阻断业务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogic {

    private final SysAuditLogService sysAuditLogService;
    private final SysUserService sysUserService;
    private final ObjectMapper objectMapper;

    /** 成功操作留痕 */
    public void success(String module, String action, String resourceType, String resourceId,
                        Map<String, Object> detail, long durationMs) {
        record(module, action, resourceType, resourceId, "SUCCESS", detail, null, durationMs);
    }

    /** 失败操作留痕 */
    public void fail(String module, String action, String resourceType, String resourceId,
                     String errorMessage, Map<String, Object> detail, long durationMs) {
        record(module, action, resourceType, resourceId, "FAIL", detail, errorMessage, durationMs);
    }

    /** 越权拦截留痕（result=DENIED） */
    public void denied(String module, String action, String resourceType, String resourceId) {
        record(module, action, resourceType, resourceId, "DENIED", null, null, 0);
    }

    public void record(String module, String action, String resourceType, String resourceId,
                       String result, Map<String, Object> detail, String errorMessage, long durationMs) {
        try {
            SysAuditLog entry = new SysAuditLog();
            entry.setModule(module);
            entry.setAction(action);
            entry.setResourceType(resourceType);
            entry.setResourceId(resourceId);
            entry.setResult(result);
            entry.setDetail(toJson(detail));
            entry.setErrorMessage(truncate(errorMessage, 500));
            entry.setLatencyMs(durationMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) durationMs);
            entry.setCreatedAt(new Date());
            fillIdentity(entry);
            sysAuditLogService.save(entry);
        } catch (Exception e) {
            log.warn("审计写入失败 module={} action={}: {}", module, action, e.getMessage());
        }
    }

    private void fillIdentity(SysAuditLog entry) {
        try {
            if (StpUtil.isLogin()) {
                Long uid = StpUtil.getLoginIdAsLong();
                entry.setUserId(uid);
                SysUser u = sysUserService.getById(uid);
                if (u != null) {
                    entry.setEmail(u.getEmail());
                }
            }
            jakarta.servlet.http.HttpServletRequest req = currentRequest();
            if (req != null) {
                entry.setIp(truncate(resolveClientIp(req), 45));
                entry.setUserAgent(truncate(req.getHeader("User-Agent"), 255));
            }
        } catch (Exception e) {
            log.debug("填充审计身份失败: {}", e.getMessage());
        }
    }

    private jakarta.servlet.http.HttpServletRequest currentRequest() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveClientIp(jakarta.servlet.http.HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private String toJson(Map<String, Object> detail) {
        if (detail == null || detail.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
