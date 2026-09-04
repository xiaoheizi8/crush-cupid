package cn.yzfy.crushcupidserver.security;

import cn.dev33.satoken.stp.StpUtil;
import cn.yzfy.crushcupidserver.exception.BizException;

/**
 * 当前登录态工具：获取登录用户 id 等。未登录时抛出 401。
 * 所有需要「当前用户」的业务隔离点都应优先走本工具，避免散落 StpUtil 调用。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 当前登录用户 id；未登录抛 401 */
    public static long currentUserId() {
        if (!StpUtil.isLogin()) {
            throw BizException.unauthorized("请先登录");
        }
        return StpUtil.getLoginIdAsLong();
    }

    /** 最近一次调用方是否已登录（不抛业务异常） */
    public static boolean isLoggedIn() {
        return StpUtil.isLogin();
    }
}
