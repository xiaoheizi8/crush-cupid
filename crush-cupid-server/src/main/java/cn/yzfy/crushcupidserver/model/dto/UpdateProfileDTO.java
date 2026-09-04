package cn.yzfy.crushcupidserver.model.dto;

import lombok.Data;

/**
 * 用户资料更新入参（null 表示不修改）
 */
@Data
public class UpdateProfileDTO {

    private String username;

    private String avatarUrl;
}
