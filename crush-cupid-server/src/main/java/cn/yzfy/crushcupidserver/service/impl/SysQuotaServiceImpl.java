package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysQuotaMapper;
import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import cn.yzfy.crushcupidserver.service.SysQuotaService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 用户配额 Service 实现
 */
@Service
public class SysQuotaServiceImpl extends ServiceImpl<SysQuotaMapper, SysQuota>
        implements SysQuotaService {

    @Override
    public SysQuota getByUserId(Long userId) {
        return getOne(new LambdaQueryWrapper<SysQuota>()
                .eq(SysQuota::getUserId, userId)
                .last("LIMIT 1"));
    }
}
