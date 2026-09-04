package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysPermMapper;
import cn.yzfy.crushcupidserver.mapper.SysRoleMapper;
import cn.yzfy.crushcupidserver.mapper.SysRolePermMapper;
import cn.yzfy.crushcupidserver.mapper.SysUserRoleMapper;
import cn.yzfy.crushcupidserver.model.entity.SysPerm;
import cn.yzfy.crushcupidserver.model.entity.SysRole;
import cn.yzfy.crushcupidserver.model.entity.SysRolePerm;
import cn.yzfy.crushcupidserver.model.entity.SysUserRole;
import cn.yzfy.crushcupidserver.service.SysRbacService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RBAC 数据访问 Service 实现（MyBatis-Plus 薄封装，只做数据访问）。
 */
@Service
@RequiredArgsConstructor
public class SysRbacServiceImpl implements SysRbacService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRolePermMapper rolePermMapper;
    private final SysPermMapper permMapper;

    @Override
    public List<String> getRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))
                .stream().map(SysRole::getCode).distinct().toList();
    }

    @Override
    public List<String> getPermCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectList(
                        new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).distinct().toList();
        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> permIds = rolePermMapper.selectList(
                        new LambdaQueryWrapper<SysRolePerm>().in(SysRolePerm::getRoleId, roleIds))
                .stream().map(SysRolePerm::getPermId).distinct().toList();
        if (permIds.isEmpty()) {
            return Collections.emptyList();
        }
        return permMapper.selectList(new LambdaQueryWrapper<SysPerm>().in(SysPerm::getId, permIds))
                .stream().map(SysPerm::getCode).distinct().toList();
    }

    @Override
    public List<SysRole> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    @Override
    public List<SysPerm> listPerms() {
        return permMapper.selectList(new LambdaQueryWrapper<SysPerm>().orderByAsc(SysPerm::getId));
    }

    @Override
    public boolean isAdmin(Long userId) {
        return getRoleCodes(userId).stream().anyMatch(code -> Objects.equals(code, "ADMIN")
                || Objects.equals(code, "ROLE_ADMIN"));
    }

    @Override
    public void assignRole(Long userId, String roleCode) {
        if (userId == null || roleCode == null) {
            return;
        }
        SysRole role = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, roleCode).last("LIMIT 1"));
        if (role == null) {
            return;
        }
        Long existing = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId).eq(SysUserRole::getRoleId, role.getId()));
        if (existing != null && existing > 0) {
            return;
        }
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        userRoleMapper.insert(ur);
    }
}
