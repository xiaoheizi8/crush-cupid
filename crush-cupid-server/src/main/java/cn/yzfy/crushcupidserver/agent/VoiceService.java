package cn.yzfy.crushcupidserver.agent;

import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.exception.BizException;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioSpeechApi;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @className VoiceService
 * @description 语音合成服务（CosyVoice 系列）。
 * <p>
 * 按 yml 配置的 model/voice 走 WebSocket 端点（wss://dashscope.aliyuncs.com/api-ws/v1/inference）。
 * Spring AI Alibaba 的模型路由白名单可能未收录最新型号，因此这里直接构建
 * {@link DashScopeAudioSpeechApi} 走 createWebSocketTask，模型名透传不经过白名单。
 * <p>
 * 专属音色通过 {@link #designVoice(String, String)} 调用百炼 voice-enrollment（声音设计）创建，
 * 每个 crush 可用一段人设描述生成专属声线，返回的 voice_id 即合成时的 voice 参数。
 * @author 一朝风月
 * @code service
 * @createTime 2026-08-27
 */
@Slf4j
@Service
public class VoiceService {

    private static final String BASE_URL = "https://dashscope.aliyuncs.com";
    private static final String WEBSOCKET_URL = "wss://dashscope.aliyuncs.com/api-ws/v1/inference";
    /** 声音设计（voice-enrollment）接口地址 */
    private static final String VOICE_DESIGN_URL = BASE_URL + "/api/v1/services/audio/tts/customization";
    /** 在线模型列表接口地址 */
    private static final String ONLINE_MODELS_URL = BASE_URL + "/api/v1/services/audio/tts/online_models";

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.audio.speech.default-options.model:cosyvoice-v3-flash}")
    private String defaultModel;

    @Value("${spring.ai.dashscope.audio.speech.default-options.voice:}")
    private String defaultVoice;

    private final ObjectMapper objectMapper;

    /** WebSocket 语音合成 API（自建实例，绕开 SDK 模型白名单路由） */
    private DashScopeAudioSpeechApi audioApi;

    /** 声音设计 REST 客户端 */
    private RestClient designClient;
    /** 在线模型列表 REST 客户端 */
    private RestClient modelsClient;

    public VoiceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (StrUtil.isBlank(apiKey)) {
            log.warn("DASHSCOPE_API_KEY 未配置，语音合成能力不可用");
            return;
        }
        try {
            this.audioApi = DashScopeAudioSpeechApi.builder()
                    .baseUrl(BASE_URL)
                    .websocketUrl(WEBSOCKET_URL)
                    .apiKey(new SimpleApiKey(apiKey))
                    .build();
            this.designClient = RestClient.builder()
                    .baseUrl(VOICE_DESIGN_URL)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
            this.modelsClient = RestClient.builder()
                    .baseUrl(BASE_URL)
                    .defaultHeader("Authorization", "Bearer " + apiKey)
                    .build();
            log.info("CosyVoice 语音合成就绪：model={}，默认音色={}", defaultModel, StrUtil.blankToDefault(defaultVoice, "未配置"));
        } catch (Exception e) {
            // 初始化失败不阻塞应用启动，降级为不可用，运行时走设计好的降级分支
            this.audioApi = null;
            this.designClient = null;
            log.warn("CosyVoice 语音初始化失败，语音能力降级不可用：{}", e.getMessage());
        }
    }

    /**
     * 合成语音（WebSocket 非流式：发送完整文本，收集全部音频帧）。
     *
     * @param text  待合成文本（不能为空）
     * @param voice 音色 ID（声音设计/复刻产生的 voice_id），空则用 yml 默认音色
     * @return mp3 字节流
     */
    public byte[] synthesize(String text, String voice) {
        if (StrUtil.isBlank(text)) {
            throw BizException.badRequest("待合成文本不能为空");
        }
        if (audioApi == null) {
            throw BizException.badRequest("语音合成不可用：未配置 DASHSCOPE_API_KEY");
        }
        String resolvedVoice = StrUtil.blankToDefault(voice, defaultVoice);
        if (StrUtil.isBlank(resolvedVoice)) {
            throw BizException.badRequest("未指定音色：v3.5 系列不支持系统音色，请先调用 /api/chat/voice/design 创建专属音色");
        }

        long start = System.currentTimeMillis();
        DashScopeAudioSpeechOptions options = DashScopeAudioSpeechOptions.builder()
                .model(defaultModel)
                .voice(resolvedVoice)
                .format("mp3")
                .build();

        byte[] audio = audioApi.createWebSocketTask(text, options)
                .map(this::toBytes)
                .collectList()
                .map(frames -> merge(frames))
                .block(Duration.ofSeconds(60));

        if (audio == null || audio.length == 0) {
            throw new BizException("语音合成失败：未收到音频数据");
        }
        log.info("CosyVoice 合成完成：{} 字符 -> {} 字节，耗时 {}ms", text.length(), audio.length, System.currentTimeMillis() - start);
        return audio;
    }

    /**
     * 声音设计：用自然语言描述创建 v3.5-plus 专属音色。
     *
     * @param voicePrompt 声音描述（如「温柔的年轻女性，语速轻快，带笑意」，≤500 字符）
     * @param previewText 预览文本（15~200 字符），空则用默认
     * @return 生成的 voice_id（用于 synthesize 的 voice 参数）
     */
    public String designVoice(String voicePrompt, String previewText) {
        if (designClient == null) {
            throw BizException.badRequest("声音设计不可用：未配置 DASHSCOPE_API_KEY");
        }
        if (StrUtil.isBlank(voicePrompt)) {
            throw BizException.badRequest("声音描述不能为空");
        }
        // 声音设计（voice_prompt 描述生成）仅 v3.5/v3 系列支持；v2 只支持上传音频的声音复刻
        if (!defaultModel.startsWith("cosyvoice-v3")) {
            throw BizException.badRequest("当前模型 " + defaultModel + " 不支持声音设计，请将模型切换为 cosyvoice-v3 系列后使用");
        }
        String preview = StrUtil.blankToDefault(previewText,
                "嘿，是我呀，今天有没有想我？记得按时吃饭，别总是熬夜到那么晚。");

        Map<String, Object> body = Map.of(
                "model", "voice-enrollment",
                "input", Map.of(
                        "action", "create_voice",
                        "target_model", defaultModel,
                        "voice_prompt", voicePrompt,
                        "preview_text", preview,
                        "prefix", "crush"),
                "parameters", Map.of("sample_rate", 24000, "response_format", "mp3"));

        String resp = designClient.post()
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);
        return extractVoiceId(resp);
    }

    /** 从声音设计响应中提取 voice_id（兼容 output / data 两层包装） */
    private String extractVoiceId(String resp) {
        try {
            JsonNode root = objectMapper.readTree(resp);
            for (String wrapper : new String[]{"output", "data"}) {
                JsonNode node = root.path(wrapper).path("voice_id");
                if (!node.isMissingNode() && StrUtil.isNotBlank(node.asText())) {
                    return node.asText();
                }
            }
            // 兜底：递归找第一个 voice_id 字段
            String found = findField(root, "voice_id");
            if (found != null) {
                return found;
            }
        } catch (Exception e) {
            log.warn("声音设计响应解析失败：{}", e.getMessage());
            throw new BizException("声音设计失败，请稍后再试");
        }
        log.warn("声音设计响应中无 voice_id：{}", resp);
        throw new BizException("声音设计失败，请稍后再试");
    }

    /** 递归查找指定字段的首个非空值 */
    private String findField(JsonNode node, String field) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (field.equals(e.getKey()) && e.getValue().isTextual() && StrUtil.isNotBlank(e.getValue().asText())) {
                    return e.getValue().asText();
                }
                String deep = findField(e.getValue(), field);
                if (deep != null) {
                    return deep;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String deep = findField(child, field);
                if (deep != null) {
                    return deep;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有可用的 TTS 模型列表。
     */
    public List<Map<String, Object>> listAvailableModels() {
        if (modelsClient == null) {
            throw BizException.badRequest("语音服务不可用：未配置 DASHSCOPE_API_KEY");
        }
        try {
            String resp = modelsClient.get()
                    .uri("/api/v1/services/audio/tts/online_models")
                    .retrieve()
                    .body(String.class);
            JsonNode root = objectMapper.readTree(resp);
            JsonNode modelsNode = root.path("output").path("models");
            if (modelsNode.isMissingNode() || !modelsNode.isArray()) {
                return List.of();
            }
            List<Map<String, Object>> models = new java.util.ArrayList<>();
            for (JsonNode modelNode : modelsNode) {
                String modelName = modelNode.path("name").asText("");
                if (modelName.startsWith("cosyvoice")) {
                    Map<String, Object> model = Map.of(
                            "name", modelName,
                            "displayName", modelNode.path("display_name").asText(modelName),
                            "voices", extractVoices(modelNode));
                    models.add(model);
                }
            }
            return models;
        } catch (Exception e) {
            log.warn("获取在线模型列表失败：{}", e.getMessage());
            // 降级：返回本地配置的默认模型
            return List.of(Map.of(
                    "name", defaultModel,
                    "displayName", defaultModel,
                    "voices", List.of()
            ));
        }
    }

    /**
     * 获取指定模型的可用音色列表。
     */
    public List<Map<String, Object>> listVoices(String model) {
        List<Map<String, Object>> models = listAvailableModels();
        for (Map<String, Object> m : models) {
            if (model.equals(m.get("name"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> voices = (List<Map<String, Object>>) m.get("voices");
                return voices != null ? voices : List.of();
            }
        }
        return List.of();
    }

    /** 从模型节点中提取音色列表 */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractVoices(JsonNode modelNode) {
        List<Map<String, Object>> voices = new java.util.ArrayList<>();
        JsonNode voicesNode = modelNode.path("voices");
        if (voicesNode.isMissingNode() || !voicesNode.isArray()) {
            return voices;
        }
        for (JsonNode v : voicesNode) {
            Map<String, Object> voice = Map.of(
                    "voiceId", v.path("name").asText(""),
                    "name", v.path("display_name").asText(v.path("name").asText("")),
                    "gender", v.path("gender").asText(""),
                    "model", modelNode.path("name").asText(""));
            voices.add(voice);
        }
        return voices;
    }

    /** 默认模型名 */
    public String getDefaultModel() { return defaultModel; }

    /** 默认音色 ID */
    public String getDefaultVoice() { return defaultVoice; }

    private byte[] toBytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private byte[] merge(java.util.List<byte[]> frames) {
        int total = frames.stream().mapToInt(f -> f.length).sum();
        byte[] merged = new byte[total];
        int pos = 0;
        for (byte[] frame : frames) {
            System.arraycopy(frame, 0, merged, pos, frame.length);
            pos += frame.length;
        }
        return merged;
    }
}
