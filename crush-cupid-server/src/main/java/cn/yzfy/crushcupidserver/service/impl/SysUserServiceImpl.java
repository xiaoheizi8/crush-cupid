package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysUserMapper;
import cn.yzfy.crushcupidserver.model.entity.SysUser;
import cn.yzfy.crushcupidserver.service.SysUserService;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户账号数据访问 Service 实现（MyBatis-Plus 薄封装，不写业务逻辑）。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
        implements SysUserService {

    @Override
    public SysUser getByEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getEmail, email.trim().toLowerCase()));
    }

    @Override
    public Page<SysUser> pageUsers(long page, long size, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .orderByDesc(SysUser::getCreatedAt);
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(SysUser::getEmail, keyword)
                    .or().like(SysUser::getUsername, keyword));
        }
        return page(new Page<>(page, size), wrapper);
    }
}
