package cn.yzfy.crushcupidserver.config;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.AiProvider;
import cn.yzfy.crushcupidserver.service.AiProviderService;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @className ChatModelRegistry
 * @description LLM 供应商注册中心。启动时合并两类来源并动态注册 OpenAI 兼容 ChatModel：
 * <ul>
 *   <li><b>系统供应商</b>：来自 {@link LlmProperties}（crush.ai.providers，YAML 只读）；</li>
 *   <li><b>自定义供应商</b>：来自 {@link AiProvider} 表（运行时增删改查，无需改配置/重启）。</li>
 * </ul>
 * 变更后调用 {@link #reload()} 即时生效；同一 key 下自定义供应商覆盖系统供应商。
 * 业务侧按 key 路由，零额外依赖（DeepSeek / 通义 / OpenAI 均走 OpenAI 兼容协议）。
 * <p>
 * 默认供应商解析：优先选择标记 {@code is_default} 的自定义供应商，否则回退 YAML 的 default-provider。
 * @author 一朝风月
 * @code registry
 * @createTime 2026-08-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatModelRegistry {

    private final LlmProperties llmProperties;
    private final AiProviderService aiProviderService;

    /**
     * Alibaba DashScope 原生 ChatModel（由 spring-ai-alibaba-starter-dashscope 自动配置注册）。
     * 用 ObjectProvider 容错：未配 DASHSCOPE_API_KEY 时不阻塞启动，仅 [qwen-native] 不可用。
     */
    private final ObjectProvider<DashScopeChatModel> dashscopeChatModelProvider;

    /** [qwen-native] 供应商代号：走 Alibaba DashScope 原生协议，拿通义全家桶 */
    public static final String QWEN_NATIVE = "qwen-native";

    /** 供应商代号 -> ChatModel 实例 */
    @Getter
    private final Map<String, ChatModel> models = new LinkedHashMap<>();

    /** 供应商代号 -> 供应商配置（供业务侧查询能力等元信息） */
    @Getter
    private final Map<String, LlmProperties.ProviderConfig> configs = new LinkedHashMap<>();

    /** 运行期默认供应商代号（DB is_default 优先，否则 YAML default-provider） */
    @Getter
    private String defaultKey;

    @PostConstruct
    public void init() {
        reload();
    }

    /**
     * 重建供应商注册表：系统 YAML + 自定义 DB 合并，并重算默认供应商。
     * 供 {@link cn.yzfy.crushcupidserver.controller.AiProviderController} 在增删改后调用以即时生效。
     */
    public synchronized void reload() {
        models.clear();
        configs.clear();

        // 1. 系统供应商（YAML，只读）
        if (llmProperties.getProviders() != null) {
            llmProperties.getProviders().forEach((key, cfg) -> {
                if (StrUtil.isBlank(cfg.getApiKey())) {
                    log.warn("系统供应商 [{}] 未配置 apiKey，跳过注册", key);
                    return;
                }
                models.put(key, buildModel(cfg));
                configs.put(key, cfg);
                log.info("注册系统 LLM 供应商 [{}] -> model={}, baseUrl={}, vision={}, audio={}",
                        key, cfg.getModel(), cfg.getBaseUrl(), cfg.isVision(), cfg.isAudio());
            });
        }

        // 2. 系统共享自定义供应商（DB，动态，user_id IS NULL）：同一 key 覆盖系统 YAML 供应商。
        //    仅用于文本对话（vision/audio 固定 false）。用户私有供应商不在此全局注册（见 UserProviderResolver）。
        List<AiProvider> dbProviders = aiProviderService.listSystem();
        boolean hasDbDefault = false;
        for (AiProvider p : dbProviders) {
            LlmProperties.ProviderConfig cfg = toProviderConfig(p);
            models.put(p.getProviderKey(), buildModel(cfg));
            configs.put(p.getProviderKey(), cfg);
            log.info("注册自定义 LLM 供应商 [{}] -> model={}, baseUrl={}, default={}",
                    p.getProviderKey(), cfg.getModel(), cfg.getBaseUrl(), Boolean.TRUE.equals(p.getIsDefault()));
            if (Boolean.TRUE.equals(p.getIsDefault())) {
                hasDbDefault = true;
            }
        }

        // 3. 默认供应商：DB is_default 优先，否则 YAML default-provider
        String dbDefault = dbProviders.stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsDefault()))
                .map(AiProvider::getProviderKey)
                .findFirst()
                .orElse(null);
        defaultKey = hasDbDefault && dbDefault != null ? dbDefault : llmProperties.getDefaultProvider();

        // 4. 探测 Alibaba DashScope 原生 ChatModel Bean，注册为 [qwen-native] 供应商
        DashScopeChatModel dashscope = dashscopeChatModelProvider.getIfAvailable();
        if (dashscope != null) {
            models.put(QWEN_NATIVE, dashscope);
            LlmProperties.ProviderConfig nativeCfg = new LlmProperties.ProviderConfig();
            nativeCfg.setBaseUrl("dashscope-native");
            nativeCfg.setModel("(alibaba-managed: qwen-plus/qwen-vl-plus/qwen-omni-turbo)");
            nativeCfg.setVision(true);
            nativeCfg.setAudio(true);
            configs.put(QWEN_NATIVE, nativeCfg);
            log.info("已注册 Alibaba DashScope 原生 ChatModel -> [{}]（通义全家桶 + 视觉/音频）", QWEN_NATIVE);
        } else {
            log.debug("未探测到 DashScopeChatModel Bean，[qwen-native] 不可用（需配置 spring.ai.dashscope.api-key）");
        }

        if (!models.containsKey(defaultKey)) {
            throw new IllegalStateException("默认 LLM 供应商 [" + defaultKey
                    + "] 未注册成功，请检查 crush.ai.providers 或自定义供应商配置");
        }
        log.info("ChatModelRegistry 重建完成，共 {} 个供应商，默认 = [{}]", models.size(), defaultKey);
    }

    /**
     * 按代号获取 ChatModel，缺省时返回默认供应商的 ChatModel。
     */
    public ChatModel get(String provider) {
        if (StrUtil.isBlank(provider)) {
            return models.get(defaultKey);
        }
        ChatModel model = models.get(provider);
        if (model == null) {
            throw BizException.badRequest("未知的 LLM 供应商：" + provider);
        }
        return model;
    }

    /** 默认供应商 ChatModel */
    public ChatModel getDefault() {
        return models.get(defaultKey);
    }

    /** 默认供应商代号 */
    public String defaultProvider() {
        return defaultKey;
    }

    /** 供应商是否支持视觉（图像理解） */
    public boolean isVision(String provider) {
        LlmProperties.ProviderConfig cfg = provider == null ? null : configs.get(provider);
        return cfg != null && cfg.isVision();
    }

    /** 供应商是否支持音频输入（语音理解） */
    public boolean isAudio(String provider) {
        LlmProperties.ProviderConfig cfg = provider == null ? null : configs.get(provider);
        return cfg != null && cfg.isAudio();
    }

    /**
     * 返回一个可用的视觉供应商代号，优先 [qwen-vl]（视觉专用），其次 [qwen-native]
     * （通义全家桶含视觉/音频），再其它声明 vision=true 的供应商。没有则返回 null。
     */
    public String firstVision() {
        String preferred = null;
        String nativeOr = null;
        for (Map.Entry<String, LlmProperties.ProviderConfig> e : configs.entrySet()) {
            if (!e.getValue().isVision()) {
                continue;
            }
            if ("qwen-vl".equals(e.getKey())) {
                return e.getKey();
            }
            if (preferred == null) {
                preferred = e.getKey();
            }
            if (QWEN_NATIVE.equals(e.getKey())) {
                nativeOr = e.getKey();
            }
        }
        return nativeOr != null ? nativeOr : preferred;
    }

    /** 自定义供应商实体 -> ProviderConfig（capabilities 字符串 → vision/audio 布尔值） */
    private LlmProperties.ProviderConfig toProviderConfig(AiProvider p) {
        LlmProperties.ProviderConfig cfg = new LlmProperties.ProviderConfig();
        BeanUtils.copyProperties(p, cfg);
        // 保留温度默认值（实体未配置时回落 0.7）
        cfg.setTemperature(p.getTemperature() != null ? p.getTemperature() : 0.7);
        // 页面配置的供应商仅用于文本对话；视觉/语音固定走 YAML 系统供应商（crush.ai.providers），
        // 避免自定义模型能力误标导致多模态请求路由到不支持视觉的模型
        cfg.setVision(false);
        cfg.setAudio(false);
        return cfg;
    }

    /**
     * 构造单个 OpenAI 兼容协议的 ChatModel。
     */
    private ChatModel buildModel(LlmProperties.ProviderConfig cfg) {
        String base = cfg.getBaseUrl();
        if (StrUtil.isNotBlank(base) && base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(base)
                .apiKey(cfg.getApiKey())
                .build();

        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(cfg.getModel())
                .temperature(cfg.getTemperature());
        if (cfg.getTopP() != null) {
            optionsBuilder.topP(cfg.getTopP());
        }
        if (cfg.getMaxTokens() != null) {
            optionsBuilder.maxTokens(cfg.getMaxTokens());
        }

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(optionsBuilder.build())
                .build();
    }
}
