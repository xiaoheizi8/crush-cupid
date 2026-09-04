package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysUser;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 用户账号数据访问 Service（MyBatis-Plus 薄封装，不写业务逻辑）。
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 按邮箱（小写）查询用户
     */
    SysUser getByEmail(String email);

    /**
     * 分页查询用户（后台用户列表，按创建时间倒序）
     */
    Page<SysUser> pageUsers(long page, long size, String keyword);
}
