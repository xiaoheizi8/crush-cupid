package cn.yzfy.crushApp.model;

/** 暗恋对象 */
public class Crush {
    public Long id;
    public String name;
    public String slug;
    public String mbti;
    public String zodiac;
    public String occupation;
    public String gender;
    public String knowDuration;
    public String relationshipStatus;
    public String impression;
    public String personaLayer0;
    public String personaLayer1;
    public String personaLayer2;
    public String personaLayer3;
    public String personaLayer4;
    public String memoryOverview;
    public String memoryTimeline;
    public String memorySweet;
    public String memoryInteraction;
    public Integer currentStage;
    public String status;
    public Integer totalMessages;
    public String lastChatDate;
    public String voiceId;
    public Boolean proactiveEnabled;
    public Integer version;
    public String createdAt;
    public String updatedAt;

    public boolean isProactiveEnabled() {
        return proactiveEnabled != null && proactiveEnabled;
    }

    public String initial() {
        if (name == null || name.isEmpty()) {
            return "?";
        }
        return name.substring(0, 1);
    }

    /** 关系阶段文案（与后端 currentStage 对齐称呼） */
    public String stageLabel() {
        if (currentStage == null) {
            return "";
        }
        String[] stages = {"初次相识", "彼此熟悉", "好感升温", "暧昧心动", "甜蜜恋爱"};
        int i = currentStage;
        if (i < 1 || i > stages.length) {
            i = 1;
        }
        return stages[i - 1];
    }
}