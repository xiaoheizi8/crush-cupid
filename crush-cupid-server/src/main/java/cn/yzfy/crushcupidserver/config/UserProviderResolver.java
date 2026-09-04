package cn.yzfy.crushcupidserver.config;

import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import cn.yzfy.crushcupidserver.security.AesGcmCrypto;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户私有 LLM 供应商的按需 ChatModel 解析与缓存。
 * <p>
 * 用户私有 key 不进全局 {@link ChatModelRegistry}（避免跨用户泄漏），而是按
 * <code>userId:providerKey</code> 独立注册/缓存，仅该用户本人可路由到自己的模型。
 * key 在调用时从 {@link AesGcmCrypto} 瞬态解密（明文只在内存、不进日志），不随任何响应下发。
 *
 * @author crush-cupid
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserProviderResolver {

    private final AiProviderService aiProviderService;
    private final AesGcmCrypto aesGcmCrypto;

    /** userId:providerKey -> ChatModel */
    private final Map<String, ChatModel> userModels = new ConcurrentHashMap<>();

    /** 用户私有供应商的路由代号（全局唯一，避免与其它用户/系统 key 冲突） */
    public String routeKey(Long userId, String providerKey) {
        return userId + ":" + providerKey;
    }

    /** 获取用户的私有供应商 ChatModel；不存在/被禁用/未配 key 时抛错 */
    public ChatModel get(Long userId, String providerKey) {
        String key = routeKey(userId, providerKey);
        ChatModel cached = userModels.get(key);
        if (cached != null) {
            return cached;
        }
        AiProvider p = aiProviderService.getUserPrivateByKey(userId, providerKey);
        if (p == null) {
            throw BizException.notFound("未找到你的私有供应商 " + providerKey);
        }
        if (p.getStatus() != null && p.getStatus() == 0) {
            throw BizException.forbidden("供应商 " + providerKey + " 已被禁用");
        }
        String apiKey = aesGcmCrypto.decrypt(p.getApiKeyEnc(), p.getApiKeyNonce());
        if (apiKey == null) {
            throw BizException.badRequest("供应商 " + providerKey + " 未配置 API Key");
        }
        ChatModel model = buildModel(p, apiKey);
        userModels.put(key, model);
        touchLastUsed(p);
        log.info("加载用户私有供应商 userId={}, providerKey={}", userId, providerKey);
        return model;
    }

    /** 供应商变更后清缓存（增删改时由 Logic 调用，下次 get 重建） */
    public void evict(Long userId, String providerKey) {
        userModels.remove(routeKey(userId, providerKey));
    }

    public void evictUser(Long userId) {
        userModels.keySet().removeIf(k -> k.startsWith(userId + ":"));
    }

    private ChatModel buildModel(AiProvider p, String apiKey) {
        String base = p.getBaseUrl();
        if (base != null && base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(base)
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                .model(p.getModel())
                .temperature(p.getTemperature() != null ? p.getTemperature() : 0.7);
        if (p.getTopP() != null) {
            options.topP(p.getTopP());
        }
        if (p.getMaxTokens() != null) {
            options.maxTokens(p.getMaxTokens());
        }
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options.build())
                .build();
    }

    private void touchLastUsed(AiProvider p) {
        try {
            p.setLastUsedAt(new Date());
            aiProviderService.updateById(p);
        } catch (Exception e) {
            log.warn("更新供应商 last_used_at 失败: {}", e.getMessage());
        }
    }
}
