package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 暗恋对象出参
 */
@Data
public class CrushVO {

    private Long id;

    private String name;

    private String slug;

    private String mbti;

    private String zodiac;

    private String occupation;

    private String gender;

    private String knowDuration;

    private String relationshipStatus;

    private String impression;

    private String personaLayer0;
    private String personaLayer1;
    private String personaLayer2;
    private String personaLayer3;
    private String personaLayer4;

    private String memoryOverview;
    private String memoryTimeline;
    private String memorySweet;
    private String memoryInteraction;

    private Integer currentStage;

    private String status;

    private Integer totalMessages;

    private Date lastChatDate;

    /** CosyVoice 专属音色 voice_id */
    private String voiceId;

    /** 是否允许 ta 主动发消息（定时调度 + 「等 ta 找我」共用开关） */
    private Boolean proactiveEnabled;

    private Integer version;

    private Date createdAt;

    private Date updatedAt;
}
