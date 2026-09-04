package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * @className AiProviderService
 * @description 自定义大模型供应商 Service（自带 MyBatis-Plus CRUD）
 * @author crush-cupid
 * @code service
 * @createTime 2026-08-31
 */
public interface AiProviderService extends IService<AiProvider> {

    /** 按供应商代号查询 */
    AiProvider getByProviderKey(String providerKey);

    /** 某用户私有的全部供应商 */
    List<AiProvider> listByUser(Long userId);

    /** 系统共享供应商（user_id IS NULL，管理员维护） */
    List<AiProvider> listSystem();

    /** 按 id + 归属用户查询（未命中该用户资源返回 null） */
    AiProvider getOwnedByUser(Long id, Long userId);

    /** 用户私有的某个 key 的供应商 */
    AiProvider getUserPrivateByKey(Long userId, String providerKey);
}
