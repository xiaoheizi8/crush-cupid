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

    /** 邮箱验证码 */
    private String code;
}
