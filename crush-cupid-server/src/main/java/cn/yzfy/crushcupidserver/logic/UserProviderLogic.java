package cn.yzfy.crushcupidserver.logic;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.ChatClientProvider;
import cn.yzfy.crushcupidserver.config.UserProviderResolver;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.converter.AiProviderConverter;
import cn.yzfy.crushcupidserver.model.dto.AiProviderDTO;
import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import cn.yzfy.crushcupidserver.model.vo.AiProviderVO;
import cn.yzfy.crushcupidserver.security.AesGcmCrypto;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户私有 LLM 供应商业务逻辑（Phase 4）：本人可 CRUD 自己的私有供应商，
 * api_key 加密落库（AES-GCM）、脱敏下发，绝不回传明文。
 * 数据访问仅委托 {@link AiProviderService}（薄封装）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProviderLogic {

    private final AiProviderService aiProviderService;
    private final AesGcmCrypto aesGcmCrypto;
    private final UserProviderResolver userProviderResolver;
    private final ChatClientProvider chatClientProvider;

    /** 当前用户的私有供应商列表（按 key 展示对应路由代号） */
    public List<AiProviderVO> list(Long userId) {
        return aiProviderService.listByUser(userId).stream()
                .map(p -> decorate(userId, AiProviderConverter.toVO(p)))
                .toList();
    }

    public AiProviderVO get(Long userId, Long id) {
        AiProvider p = aiProviderService.getOwnedByUser(id, userId);
        if (p == null) {
            throw BizException.notFound("未找到你的私有供应商 id=" + id);
        }
        return decorate(userId, AiProviderConverter.toVO(p));
    }

    public AiProviderVO create(Long userId, AiProviderDTO dto) {
        validate(dto);
        if (aiProviderService.getUserPrivateByKey(userId, dto.getProviderKey()) != null) {
            throw BizException.badRequest("供应商代号已存在：" + dto.getProviderKey());
        }
        AiProvider entity = toEncryptedEntity(userId, dto);
        entity.setStatus(1);
        aiProviderService.save(entity);
        return decorate(userId, AiProviderConverter.toVO(entity));
    }

    public AiProviderVO update(Long userId, Long id, AiProviderDTO dto) {
        AiProvider entity = aiProviderService.getOwnedByUser(id, userId);
        if (entity == null) {
            throw BizException.notFound("未找到你的私有供应商 id=" + id);
        }
        if (StrUtil.isNotBlank(dto.getProviderKey())
                && !dto.getProviderKey().equals(entity.getProviderKey())) {
            AiProvider dup = aiProviderService.getUserPrivateByKey(userId, dto.getProviderKey());
            if (dup != null && !dup.getId().equals(id)) {
                throw BizException.badRequest("供应商代号已存在：" + dto.getProviderKey());
            }
        }
        applyFields(entity, dto);
        // 仅当提交了新 apiKey 才更新加密 key
        if (StrUtil.isNotBlank(dto.getApiKey())) {
            encryptKey(entity, dto.getApiKey());
        }
        entity.setUpdatedAt(new Date());
        aiProviderService.updateById(entity);
        // 清空 ChatClient 缓存（按 {uid}:{key} 缓存的 ChatClient 绑定旧模型/旧 key），
        // 并清掉解析器里该用户的缓存，避免沿用旧 key 或旧 providerKey。
        userProviderResolver.evictUser(userId);
        chatClientProvider.refresh();
        return decorate(userId, AiProviderConverter.toVO(entity));
    }

    public void delete(Long userId, Long id) {
        AiProvider entity = aiProviderService.getOwnedByUser(id, userId);
        if (entity == null) {
            throw BizException.notFound("未找到你的私有供应商 id=" + id);
        }
        aiProviderService.removeById(id);
        userProviderResolver.evictUser(userId);
        chatClientProvider.refresh();
    }

    // ---------------- 内部 ----------------

    private void validate(AiProviderDTO dto) {
        if (StrUtil.isBlank(dto.getName())) {
            throw BizException.badRequest("name 不能为空");
        }
        if (StrUtil.isBlank(dto.getProviderKey())) {
            throw BizException.badRequest("providerKey（供应商代号）不能为空");
        }
        if (StrUtil.isBlank(dto.getBaseUrl())) {
            throw BizException.badRequest("baseUrl 不能为空");
        }
        if (StrUtil.isBlank(dto.getModel())) {
            throw BizException.badRequest("model 不能为空");
        }
    }

    private AiProvider toEncryptedEntity(Long userId, AiProviderDTO dto) {
        AiProvider p = new AiProvider();
        p.setUserId(userId);
        applyFields(p, dto);
        if (StrUtil.isNotBlank(dto.getApiKey())) {
            encryptKey(p, dto.getApiKey());
        }
        p.setCreatedAt(new Date());
        p.setUpdatedAt(new Date());
        return p;
    }

    private void applyFields(AiProvider entity, AiProviderDTO dto) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getProviderKey() != null) entity.setProviderKey(dto.getProviderKey());
        if (dto.getBaseUrl() != null) entity.setBaseUrl(dto.getBaseUrl());
        if (dto.getModel() != null) entity.setModel(dto.getModel());
        if (dto.getTemperature() != null) entity.setTemperature(dto.getTemperature());
        if (dto.getTopP() != null) entity.setTopP(dto.getTopP());
        if (dto.getMaxTokens() != null) entity.setMaxTokens(dto.getMaxTokens());
        if (dto.getCapabilities() != null) {
            entity.setCapabilities(AiProviderConverter.joinCapabilities(dto.getCapabilities()));
        }
    }

    private void encryptKey(AiProvider entity, String plaintext) {
        byte[] nonce = aesGcmCrypto.randomNonce();
        entity.setApiKeyEnc(aesGcmCrypto.encrypt(plaintext, nonce));
        entity.setApiKeyNonce(nonce);
        entity.setApiKeyMask(AiProviderConverter.maskKey(plaintext));
        // 用户私有：明文列不写
        entity.setApiKey(null);
    }

    private AiProviderVO decorate(Long userId, AiProviderVO vo) {
        if (StrUtil.isBlank(vo.getProviderKey())) {
            return vo;
        }
        vo.setProviderKey(userProviderResolver.routeKey(userId, vo.getProviderKey()));
        return vo;
    }
}
