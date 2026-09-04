package cn.yzfy.crushcupidserver.security;

import cn.dev33.satoken.stp.StpInterface;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 角色/权限动态加载接口（Phase 3：从 sys_user_role / sys_role / sys_perm 表加载）。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysRbacService sysRbacService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return sysRbacService.getPermCodes(toUserId(loginId));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysRbacService.getRoleCodes(toUserId(loginId));
    }

    private Long toUserId(Object loginId) {
        try {
            return Long.valueOf(String.valueOf(loginId));
        } catch (NumberFormatException e) {
            return -1L;
        }
    }
}
