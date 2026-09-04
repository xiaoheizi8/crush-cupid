package cn.yzfy.crushcupidserver.logic;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.config.ChatClientProvider;
import cn.yzfy.crushcupidserver.config.ChatModelRegistry;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.converter.AiProviderConverter;
import cn.yzfy.crushcupidserver.model.dto.AiProviderDTO;
import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import cn.yzfy.crushcupidserver.model.vo.AiProviderVO;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 自定义大模型 API 供应商业务逻辑层：校验、默认位互斥、变更后刷新注册表与客户端缓存。
 * 数据访问仅委托 {@link AiProviderService}（MyBatis-Plus 薄封装）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiProviderLogic {

    private final AiProviderService aiProviderService;
    private final ChatModelRegistry chatModelRegistry;
    private final ChatClientProvider chatClientProvider;

    public List<AiProviderVO> list() {
        return aiProviderService.list().stream()
                .map(AiProviderConverter::toVO)
                .toList();
    }

    public AiProviderVO get(Long id) {
        AiProvider p = aiProviderService.getById(id);
        if (p == null) {
            throw BizException.notFound("未找到自定义供应商 id=" + id);
        }
        return AiProviderConverter.toVO(p);
    }

    public AiProviderVO create(AiProviderDTO dto) {
        validate(dto);
        if (aiProviderService.getByProviderKey(dto.getProviderKey()) != null) {
            throw BizException.badRequest("供应商代号已存在：" + dto.getProviderKey());
        }
        AiProvider entity = AiProviderConverter.toEntity(dto);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultFlags(entity.getId());
        }
        aiProviderService.save(entity);
        refresh();
        return AiProviderConverter.toVO(entity);
    }

    public AiProviderVO update(Long id, AiProviderDTO dto) {
        AiProvider entity = aiProviderService.getById(id);
        if (entity == null) {
            throw BizException.notFound("未找到自定义供应商 id=" + id);
        }
        // 如修改了 providerKey，需校验唯一（排除自身）
        if (StrUtil.isNotBlank(dto.getProviderKey())
                && !dto.getProviderKey().equals(entity.getProviderKey())
                && aiProviderService.getByProviderKey(dto.getProviderKey()) != null) {
            throw BizException.badRequest("供应商代号已存在：" + dto.getProviderKey());
        }
        performValidation(entity, dto);
        if (Boolean.TRUE.equals(dto.getIsDefault())) {
            clearDefaultFlags(id);
        }
        AiProviderConverter.apply(entity, dto);
        entity.setUpdatedAt(new Date());
        aiProviderService.updateById(entity);
        refresh();
        return AiProviderConverter.toVO(entity);
    }

    public void delete(Long id) {
        aiProviderService.removeById(id);
        refresh();
    }

    /** 把其它供应商的 is_default 清零（同一时刻只能一个默认自定义供应商） */
    private void clearDefaultFlags(Long excludeId) {
        aiProviderService.lambdaUpdate()
                .eq(AiProvider::getIsDefault, true)
                .ne(AiProvider::getId, excludeId)
                .set(AiProvider::getIsDefault, false)
                .update();
    }

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

    /** 更新时：只对将要生效的字段做非空校验，允许部分更新 */
    private void performValidation(AiProvider entity, AiProviderDTO dto) {
        if (dto.getName() != null && dto.getName().isBlank()) {
            throw BizException.badRequest("name 不能为空");
        }
        if (dto.getProviderKey() != null && dto.getProviderKey().isBlank()) {
            throw BizException.badRequest("providerKey 不能为空");
        }
        if (dto.getBaseUrl() != null && dto.getBaseUrl().isBlank()) {
            throw BizException.badRequest("baseUrl 不能为空");
        }
        if (dto.getModel() != null && dto.getModel().isBlank()) {
            throw BizException.badRequest("model 不能为空");
        }
        // 若某必填字段本次不提供修改，则实体上必须已存在
        if (dto.getBaseUrl() == null && StrUtil.isBlank(entity.getBaseUrl())) {
            throw BizException.badRequest("baseUrl 不能为空");
        }
        if (dto.getModel() == null && StrUtil.isBlank(entity.getModel())) {
            throw BizException.badRequest("model 不能为空");
        }
    }

    /** 变更后刷新注册表与 ChatClient 缓存，即时生效 */
    private void refresh() {
        try {
            chatModelRegistry.reload();
        } catch (IllegalStateException e) {
            log.warn("供应商变更后重建失败：{}", e.getMessage());
            throw BizException.badRequest(e.getMessage());
        }
        chatClientProvider.refresh();
    }
}