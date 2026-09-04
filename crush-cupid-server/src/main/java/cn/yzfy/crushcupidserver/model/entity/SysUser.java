package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户账号（邮箱注册认证）
 *
 * @TableName sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 登录名（邮箱，入库前转小写） */
    private String email;

    /** 昵称，可空 */
    private String username;

    /** BCrypt 密码哈希 */
    private String passwordHash;

    private String avatarUrl;

    /** 1启用 0禁用 -1锁定 */
    private Integer status;

    /** 邮箱是否已验证 */
    private Boolean emailVerified;

    /** 连续失败次数 */
    private Integer failedAttempt;

    /** 锁定截至时间 */
    private Date lockedUntil;

    private Date lastLoginAt;

    private String lastLoginIp;

    private Date createdAt;

    private Date updatedAt;
}