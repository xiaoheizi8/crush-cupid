package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户-角色关联
 *
 * @TableName sys_user_role
 */
@Data
@TableName("sys_user_role")
public class SysUserRole implements Serializable {

    private Long userId;

    private Long roleId;
}
