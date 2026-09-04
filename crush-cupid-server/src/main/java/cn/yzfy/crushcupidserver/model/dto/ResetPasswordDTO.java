package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 重置密码请求（验证码方式）
 */
@Data
public class ResetPasswordDTO {

    /** 邮箱 */
    private String email;

    /** 邮箱验证码 */
    private String code;

    /** 新密码 */
    private String newPassword;
}
