package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import cn.yzfy.crushcupidserver.mapper.AiProviderMapper;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @className AiProviderServiceImpl
 * @description 自定义大模型供应商 Service 实现
 * @author crush-cupid
 * @code service impl
 * @createTime 2026-08-31
 */
@Service
public class AiProviderServiceImpl extends ServiceImpl<AiProviderMapper, AiProvider>
        implements AiProviderService {

    @Override
    public AiProvider getByProviderKey(String providerKey) {
        return getOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getProviderKey, providerKey)
                .last("LIMIT 1"));
    }

    @Override
    public List<AiProvider> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getUserId, userId)
                .orderByAsc(AiProvider::getId));
    }

    @Override
    public List<AiProvider> listSystem() {
        return list(new LambdaQueryWrapper<AiProvider>()
                .isNull(AiProvider::getUserId)
                .orderByAsc(AiProvider::getId));
    }

    @Override
    public AiProvider getOwnedByUser(Long id, Long userId) {
        return getOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getId, id)
                .eq(AiProvider::getUserId, userId)
                .last("LIMIT 1"));
    }

    @Override
    public AiProvider getUserPrivateByKey(Long userId, String providerKey) {
        return getOne(new LambdaQueryWrapper<AiProvider>()
                .eq(AiProvider::getUserId, userId)
                .eq(AiProvider::getProviderKey, providerKey)
                .last("LIMIT 1"));
    }
}
