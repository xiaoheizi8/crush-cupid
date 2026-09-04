package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysUserKeyMapper;
import cn.yzfy.crushcupidserver.model.entity.SysUserKey;
import cn.yzfy.crushcupidserver.service.SysUserKeyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * sys_user_key 数据访问实现。
 */
@Service
public class SysUserKeyServiceImpl extends ServiceImpl<SysUserKeyMapper, SysUserKey>
        implements SysUserKeyService {

    @Override
    public SysUserKey getByUserId(long userId) {
        return getOne(new LambdaQueryWrapper<SysUserKey>()
                .eq(SysUserKey::getUserId, userId)
                .last("LIMIT 1"));
    }
}
