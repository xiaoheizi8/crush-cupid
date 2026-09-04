package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 邮箱注册请求
 */
@Data
public class RegisterDTO {

    /** 邮箱 */
    private String email;

    /** 密码（8~64 位，含大小写/数字） */
    private String password;

    /** 邮箱验证码 */
    private String code;
}
