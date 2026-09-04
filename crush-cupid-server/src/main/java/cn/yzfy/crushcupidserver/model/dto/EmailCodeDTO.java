package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 发送邮箱验证码请求
 */
@Data
public class EmailCodeDTO {

    /** 邮箱 */
    private String email;

    /** 用途：REGISTER / LOGIN / RESET_PWD */
    private String purpose;
}
