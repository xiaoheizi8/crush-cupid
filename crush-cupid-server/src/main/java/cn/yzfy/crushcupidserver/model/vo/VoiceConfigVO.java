package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class VoiceConfigVO {

    private String currentVoice;
    private String currentModel;
    private List<Map<String, Object>> availableModels;
    private List<VoiceOption> availableVoices;

    @Data
    public static class VoiceOption {
        private String voiceId;
        private String name;
        private String model;
        private String gender;
    }
}
