package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 权限点
 *
 * @TableName sys_perm
 */
@Data
@TableName("sys_perm")
public class SysPerm implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 权限编码，如 crush:read / admin:user:read */
    private String code;

    private String name;

    /** API / MENU / BUTTON */
    private String type;

    /** 所属模块 */
    private String module;

    private Date createdAt;
}
