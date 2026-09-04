package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysPerm;
import cn.yzfy.crushcupidserver.model.entity.SysRole;

import java.util.List;

/**
 * RBAC 数据访问 Service（MyBatis-Plus 薄封装，不写业务逻辑）。
 * 负责用户 → 角色码 / 权限码 的关联查询，以及角色/权限枚举的只读查询。
 */
public interface SysRbacService {

    /** 用户拥有的角色码列表（如 [ADMIN] / [USER]） */
    List<String> getRoleCodes(Long userId);

    /** 用户拥有的权限码列表（通过 用户-角色-权限 关联） */
    List<String> getPermCodes(Long userId);

    /** 全部角色（/api/meta/roles） */
    List<SysRole> listRoles();

    /** 全部权限点（/api/meta/permissions） */
    List<SysPerm> listPerms();

    /** 是否为 ADMIN 角色 */
    boolean isAdmin(Long userId);

    /** 为用户分配一个角色（幂等，角色不存在则忽略） */
    void assignRole(Long userId, String roleCode);
}
