package cn.yzfy.crushcupidserver.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(500, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException notFound(String message) {
        return new BizException(404, message);
    }

    public static BizException badRequest(String message) {
        return new BizException(400, message);
    }

    /** 未登录 / token 失效 */
    public static BizException unauthorized(String message) {
        return new BizException(401, message);
    }

    /** 无权限 / 非本人资源 */
    public static BizException forbidden(String message) {
        return new BizException(403, message);
    }

    /** 认证模块业务错误（邮箱格式 1001、已注册 1002、验证码 1003、密码强度 1004、账号锁定 1005、禁用 1006） */
    public static BizException authError(int subCode, String message) {
        return new BizException(1000 + subCode, message);
    }
}
