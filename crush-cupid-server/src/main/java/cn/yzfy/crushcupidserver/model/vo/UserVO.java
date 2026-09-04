package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户信息视图
 */
@Data
public class UserVO {

    private Long id;

    private String email;

    private String username;

    private String avatarUrl;

    private Boolean emailVerified;

    private Date createdAt;
}
