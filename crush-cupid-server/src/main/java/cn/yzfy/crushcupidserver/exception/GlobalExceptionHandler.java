package cn.yzfy.crushcupidserver.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.exception.SaTokenException;
import cn.yzfy.crushcupidserver.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 注意：SSE 端点（chat/advisor/build/push/listen）的 produces 为 text/event-stream，
 * 若异常响应沿用该 Content-Type，Spring 找不到 converter 序列化 {@link Result} JSON，
 * 会二次抛出 HttpMessageNotWritableException。故所有异常响应统一强制 application/json。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        return json(HttpStatus.OK, e.getCode(), e.getMessage());
    }

    /** 未登录：校验失败/登录态失效 -> HTTP 401 */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<Result<Void>> handleNotLogin(NotLoginException e) {
        return json(HttpStatus.UNAUTHORIZED, 401, "未登录或登录已过期");
    }

    /** 缺少角色 -> HTTP 403 */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<Result<Void>> handleNotRole(NotRoleException e) {
        return json(HttpStatus.FORBIDDEN, 403, "无权限执行该操作");
    }

    /** 缺少权限 -> HTTP 403 */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<Result<Void>> handleNotPermission(NotPermissionException e) {
        return json(HttpStatus.FORBIDDEN, 403, "无权限执行该操作");
    }

    /** 其他 Sa-Token 异常（如账号被禁用）-> HTTP 400 */
    @ExceptionHandler(SaTokenException.class)
    public ResponseEntity<Result<Void>> handleSaToken(SaTokenException e) {
        return json(HttpStatus.BAD_REQUEST, 400, "认证状态异常");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return json(HttpStatus.INTERNAL_SERVER_ERROR, 500, "服务器开小差了，请稍后再试");
    }

    /** 构造 JSON 响应：显式设置 application/json，覆盖 SSE 端点的 text/event-stream produces */
    private ResponseEntity<Result<Void>> json(HttpStatus status, int code, String msg) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new ResponseEntity<>(Result.fail(code, msg), headers, status);
    }
}
