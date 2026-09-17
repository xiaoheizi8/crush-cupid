package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.yzfy.crushApp.model.VoiceConfig;

/** 语音合成 / 声音设计 / 音色配置 */
public final class VoiceApi {

    private static final Type STRING_T = new TypeToken<Result<String>>() {
    }.getType();
    private static final Type CONFIG_T = new TypeToken<Result<VoiceConfig>>() {
    }.getType();
    private static final Type MODELS_T = new TypeToken<Result<List<VoiceConfig.VoiceModel>>>() {
    }.getType();
    private static final Type VOICES_T = new TypeToken<Result<List<VoiceConfig.VoiceOption>>>() {
    }.getType();

    private static final String VOICE_PREFS = "preferredVoice";

    private VoiceApi() {
    }

    /** 文本 → mp3 base64 */
    public static void synthesize(String text, String voice, Rest.Callback<String> cb) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("text", text);
        if (voice != null && !voice.isEmpty()) {
            body.put("voice", voice);
        }
        Rest.post("/api/chat/voice", body, STRING_T, cb);
    }

    /** 人设描述 → 专属 voice_id */
    public static void design(String voicePrompt, String previewText, Rest.Callback<String> cb) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("voicePrompt", voicePrompt);
        if (previewText != null && !previewText.isEmpty()) {
            body.put("previewText", previewText);
        }
        Rest.post("/api/chat/voice/design", body, STRING_T, cb);
    }

    /** 当前用户音色配置 */
    public static void getConfig(Rest.Callback<VoiceConfig> cb) {
        Rest.get("/api/chat/voice/config", CONFIG_T, cb);
    }

    /** 保存偏好音色（随后合成使用） */
    public static void saveConfig(String preferredVoice, Rest.Callback<VoiceConfig> cb) {
        Map<String, String> body = new HashMap<>();
        body.put("preferredVoice", preferredVoice == null ? "" : preferredVoice);
        Rest.post("/api/chat/voice/config", body, CONFIG_T, cb);
    }

    /** 全部在线 TTS 模型（含各自音色） */
    public static void listModels(Rest.Callback<List<VoiceConfig.VoiceModel>> cb) {
        Rest.get("/api/chat/voice/models", MODELS_T, cb);
    }

    /** 指定模型的可用音色 */
    public static void listVoices(String model, Rest.Callback<List<VoiceConfig.VoiceOption>> cb) {
        Rest.get("/api/chat/voice/voices?model=" + (model == null ? "" : model), VOICES_T, cb);
    }

    /** 本地缓存的偏好音色（聊天页直接使用） */
    public static String cachedVoice() {
        return AppPrefs.getString(VOICE_PREFS, null);
    }

    /** 更新本地缓存的偏好音色 */
    public static void cacheVoice(String voice) {
        if (voice == null || voice.isEmpty()) {
            AppPrefs.remove(VOICE_PREFS);
        } else {
            AppPrefs.putString(VOICE_PREFS, voice);
        }
    }
}