package cn.yzfy.crushApp.model;

import java.util.List;

/** 音色配置（对应后端 VoiceConfigVO）。 */
public class VoiceConfig {
    public String currentVoice;
    public String currentModel;
    public List<VoiceModel> availableModels;
    public List<VoiceOption> availableVoices;

    public static class VoiceModel {
        public String name;
        public String displayName;
        public List<VoiceOption> voices;
    }

    public static class VoiceOption {
        public String voiceId;
        public String name;
        public String gender;
        public String model;
    }
}