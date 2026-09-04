package cn.yzfy.crushcupidserver.logic;

import cn.dev33.satoken.stp.StpUtil;
import cn.yzfy.crushcupidserver.model.entity.SysPerm;
import cn.yzfy.crushcupidserver.model.entity.SysRole;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 元数据业务逻辑：返回当前用户角色/权限枚举（供后台前端拉取）。
 */
@Service
@RequiredArgsConstructor
public class MetaLogic {

    private final SysRbacService sysRbacService;

    /** 当前用户权限；全量枚举仅 ADMIN 可见（避免向普通用户泄露后台权限点） */
    public Map<String, Object> permissions() {
        long userId = currentUserId();
        List<String> owned = sysRbacService.getPermCodes(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("owned", owned);
        if (sysRbacService.isAdmin(userId)) {
            body.put("all", sysRbacService.listPerms().stream().map(SysPerm::getCode).toList());
        }
        return body;
    }

    /** 当前用户角色码；全量角色仅 ADMIN 可见 */
    public Map<String, Object> roles() {
        long userId = currentUserId();
        List<String> owned = sysRbacService.getRoleCodes(userId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("owned", owned);
        if (sysRbacService.isAdmin(userId)) {
            body.put("all", sysRbacService.listRoles());
        }
        return body;
    }

    private long currentUserId() {
        if (!StpUtil.isLogin()) {
            return -1L;
        }
        return StpUtil.getLoginIdAsLong();
    }
}
