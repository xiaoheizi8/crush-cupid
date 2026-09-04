package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 邮箱验证码（注册 / 找回密码）
 *
 * @TableName sys_email_code
 */
@Data
@TableName("sys_email_code")
public class SysEmailCode implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 邮箱（小写） */
    private String email;

    /** REGISTER / RESET_PWD */
    private String purpose;

    /** SHA-256(code) */
    private String codeHash;

    private Date expireAt;

    /** 是否已使用 */
    private Boolean used;

    /** 校验失败次数（超 5 次作废） */
    private Integer attempt;

    private Date createdAt;
}