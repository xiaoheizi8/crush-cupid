package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysConfigMapper;
import cn.yzfy.crushcupidserver.model.entity.SysConfig;
import cn.yzfy.crushcupidserver.service.SysConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 系统键值配置 Service 实现
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig>
        implements SysConfigService {

    @Override
    public String getValue(String key, String fallback) {
        SysConfig cfg = getOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, key)
                .last("LIMIT 1"));
        return cfg == null || cfg.getConfigValue() == null ? fallback : cfg.getConfigValue();
    }
}
