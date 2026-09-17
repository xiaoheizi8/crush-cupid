package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 邮箱登录请求
 */
@Data
public class LoginDTO {

    /** 邮箱 */
    private String email;

    /** 密码 */
    private String password;

    /** 图形验证码实例 ID（由 GET /api/auth/captcha 获取） */
    private String captchaId;

    /** 图形验证码输入 */
    private String captcha;
}
