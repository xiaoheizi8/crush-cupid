package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * 用户配额数据访问 Service
 */
public interface SysQuotaService extends IService<SysQuota> {

    /** 按用户查配额；无则 null */
    SysQuota getByUserId(Long userId);
}
