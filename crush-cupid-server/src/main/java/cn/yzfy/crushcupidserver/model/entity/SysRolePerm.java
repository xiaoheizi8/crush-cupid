package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色-权限关联
 *
 * @TableName sys_role_perm
 */
@Data
@TableName("sys_role_perm")
public class SysRolePerm implements Serializable {

    private Long roleId;

    private Long permId;
}
