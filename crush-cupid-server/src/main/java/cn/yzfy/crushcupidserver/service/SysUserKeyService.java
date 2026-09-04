package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysUserKey;
import com.baomidou.mybatisplus.spring.service.IService;

/**
 * sys_user_key 数据访问（每用户聊天加密密钥）。
 */
public interface SysUserKeyService extends IService<SysUserKey> {

    /** 按用户查询密钥行（无则 null） */
    SysUserKey getByUserId(long userId);
}
