package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 修改密码请求（登录后）
 */
@Data
public class ChangePasswordDTO {

    /** 原密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;
}
