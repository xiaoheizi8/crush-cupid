package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 后台用户管理视图
 */
@Data
public class AdminUserVO {

    private Long id;
    private String email;
    private String username;
    private Integer status;
    private Boolean emailVerified;
    private Date createdAt;
    private Date lastLoginAt;
    private List<String> roles;
    private String plan;
}
