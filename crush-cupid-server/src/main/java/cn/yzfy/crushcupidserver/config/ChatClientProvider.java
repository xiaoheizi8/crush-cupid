package cn.yzfy.crushcupidserver.config;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.agent.advisor.SafetyAdvisor;
import cn.yzfy.crushcupidserver.agent.tool.CrushTools;
import cn.yzfy.crushcupidserver.agent.tool.OcrTools;
import cn.yzfy.crushcupidserver.agent.tool.StickerTools;
import cn.yzfy.crushcupidserver.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className ChatClientProvider
 * @description ChatClient 路由提供者。按供应商代号构造并缓存 {@link ChatClient}，
 * 每个 ChatClient 共享同一套 advisor（memory/safety）与工具回调，但绑定不同的底层 ChatModel。
 * <p>
 * 业务侧（如 CupidAgent）按 crush 或请求级 provider 选择 ChatClient，实现「一个对话用某个供应商」。
 * @author 一朝风月
 * @code provider
 * @createTime 2026-08-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatClientProvider {

    private final ChatModelRegistry chatModelRegistry;
    private final UserProviderResolver userProviderResolver;
    private final SafetyAdvisor safetyAdvisor;
    private final CrushTools crushTools;
    private final OcrTools ocrTools;
    private final StickerTools stickerTools;

    /** 供应商代号 -> ChatClient（懒加载，线程安全由 ConcurrentHashMap 保证） */
    private final Map<String, ChatClient> clients = new ConcurrentHashMap<>();

    /** 本地 @Tool 方法回调，所有 ChatClient 共享 */
    private ToolCallbackProvider methodToolCallbackProvider;

    /** 工具回调（本地 @Tool），所有 ChatClient 共享 */
    private ToolCallback[] allToolCallbacks;

    @PostConstruct
    public void init() {
        this.methodToolCallbackProvider = MethodToolCallbackProvider.builder()
                .toolObjects(crushTools, ocrTools)
                .build();
        this.allToolCallbacks = methodToolCallbackProvider.getToolCallbacks();
        getOrCreate(chatModelRegistry.defaultProvider());
    }

    /**
     * 按代号获取 ChatClient。缺省回退到默认供应商。
     */
    public ChatClient get(String provider) {
        String key = StrUtil.isBlank(provider) ? chatModelRegistry.defaultProvider() : provider;
        return getOrCreate(key);
    }

    /** 默认供应商 ChatClient */
    public ChatClient getDefault() {
        return getOrCreate(chatModelRegistry.defaultProvider());
    }

    private ChatClient getOrCreate(String provider) {
        return clients.computeIfAbsent(provider, this::buildClient);
    }

    /**
     * 供应商运行时变更后清空 ChatClient 缓存，使下次 {@link #get}/{@link #getDefault}
     * 基于最新的 ChatModel 重建。由 {@link AiProviderController} 在增删改后调用。
     */
    public void refresh() {
        clients.clear();
    }

    private ChatClient buildClient(String provider) {
        ChatModel chatModel = resolveChatModel(provider);
        return ChatClient.builder(chatModel)
                .defaultAdvisors(safetyAdvisor)
                .defaultToolCallbacks(allToolCallbacks)
                .build();
    }

    /**
     * 解析 provider 对应的 ChatModel：
     * <ul>
     *   <li>用户私有的路由代号 {@code {userId}:{providerKey}}：校验调用方即本人后，
     *       在请求线程瞬态解密其私有 key 并构建模型（{@link UserProviderResolver}）；</li>
     *   <li>否则走全局 {@link ChatModelRegistry}（系统/YAML 供应商）。</li>
     * </ul>
     * 仅在请求线程调用（此时 Sa-Token 登录上下文在 ThreadLocal 上），故越权校验是同步且安全的。
     */
    private ChatModel resolveChatModel(String provider) {
        PrivateRoute privateRoute = tryParsePrivate(provider);
        if (privateRoute != null) {
            return userProviderResolver.get(privateRoute.userId, privateRoute.providerKey);
        }
        return chatModelRegistry.get(provider);
    }

    /** 解析私有路由代号；非法/非私有返回 null。userId 必须等于当前登录用户，杜绝跨用户调用。 */
    private PrivateRoute tryParsePrivate(String provider) {
        if (StrUtil.isBlank(provider)) {
            return null;
        }
        int colon = provider.indexOf(':');
        if (colon <= 0 || colon == provider.length() - 1) {
            return null;
        }
        String uidStr = provider.substring(0, colon);
        if (!uidStr.chars().allMatch(Character::isDigit)) {
            return null;
        }
        long uid;
        try {
            uid = Long.parseLong(uidStr);
        } catch (NumberFormatException e) {
            return null;
        }
        long current = currentUserIdSafe();
        if (current != uid) {
            throw BizException.forbidden("无权使用该私有供应商");
        }
        return new PrivateRoute(uid, provider.substring(colon + 1));
    }

    private long currentUserIdSafe() {
        try {
            return cn.yzfy.crushcupidserver.security.SecurityUtils.currentUserId();
        } catch (Exception e) {
            return -1L;
        }
    }

    private record PrivateRoute(long userId, String providerKey) {
    }

    /**
     * 校验供应商是否支持视觉（图像理解），便于在视觉请求时给业务层提示。
     */
    public void ensureVision(String provider) {
        if (!chatModelRegistry.isVision(provider)) {
            throw BizException.badRequest("供应商 [" + provider + "] 不支持视觉（图像理解），请切换到 qwen-vl / gpt-4o 等");
        }
    }

    /**
     * 供应商是否声明支持视觉（vision）。
     * 聊天发图按此分流：视觉模型直传原图走图像理解；非视觉降级 OCR 提取文字。
     */
    public boolean isVision(String provider) {
        return chatModelRegistry.isVision(provider);
    }

    /**
     * 供应商是否声明支持音频输入（audio）。
     */
    public boolean isAudio(String provider) {
        return chatModelRegistry.isAudio(provider);
    }

    /**
     * 解析最终生效的供应商代号：
     * <ul>
     *   <li>请求带图片 media 且当前供应商非视觉时，自动切换到已注册的视觉模型
     *       （优先 qwen-vl / qwen-native），让模型真正"看懂"聊天图片；</li>
     *   <li>否则维持请求指定或默认供应商。</li>
     * </ul>
     */
    public String resolveProvider(String requestedProvider, boolean hasImageMedia) {
        String base = StrUtil.isBlank(requestedProvider) ? chatModelRegistry.defaultProvider() : requestedProvider;
        if (!hasImageMedia || chatModelRegistry.isVision(base)) {
            return base;
        }
        String visionProvider = chatModelRegistry.firstVision();
        if (visionProvider == null || visionProvider.equals(base)) {
            throw BizException.badRequest("当前供应商 [" + base + "] 不支持图片，且未配置任何视觉模型");
        }
        log.info("请求带图片但供应商 [{}] 非视觉，自动切换到 [{}] 视觉模型", base, visionProvider);
        return visionProvider;
    }
}
