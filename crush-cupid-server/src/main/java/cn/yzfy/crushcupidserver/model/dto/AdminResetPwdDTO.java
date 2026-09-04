package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 管理员重置用户密码入参
 */
@Data
public class AdminResetPwdDTO {

    /** 新密码（8~64 位，含大小写字母与数字） */
    private String newPassword;
}
