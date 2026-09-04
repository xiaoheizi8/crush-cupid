package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色
 *
 * @TableName sys_role
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 角色编码，如 ROLE_USER / ROLE_ADMIN / ROLE_OPERATOR */
    private String code;

    private String name;

    private String description;

    /** 内置角色不可删 */
    private Boolean builtin;

    private Date createdAt;
}
