package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysConfig;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 系统键值配置数据访问 Service
 */
public interface SysConfigService extends IService<SysConfig> {

    /** 读取配置值；缺省返回 fallback */
    String getValue(String key, String fallback);
}
