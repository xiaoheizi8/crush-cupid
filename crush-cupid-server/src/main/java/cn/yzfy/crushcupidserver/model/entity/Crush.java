package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 暗恋对象
 *
 * @TableName crush
 */
@Data
@TableName("crush")
public class Crush  implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 归属用户 id（0=系统共享/演示，见 SecurityContext OwnershipGuard） */
    private Long userId;

    /** 花名/代号 */
    private String name;

    /** 唯一标识 slug */
    private String slug;

    private String mbti;

    private String zodiac;

    private String occupation;

    private String gender;

    /** 认识时长 */
    private String knowDuration;

    /** 关系状态 */
    private String relationshipStatus;

    /** 主观印象 */
    private String impression;

    /** Persona 5 层 */
    private String personaLayer0;
    private String personaLayer1;
    private String personaLayer2;
    private String personaLayer3;
    private String personaLayer4;

    /** 关系记忆 */
    private String memoryOverview;
    private String memoryTimeline;
    private String memorySweet;
    private String memoryInteraction;

    /** 当前进展阶段 */
    private Integer currentStage;

    /** 构建状态：DRAFT（仅基础信息） / READY（已生成 persona/memory） */
    private String status;

    private Integer totalMessages;

    private Date lastChatDate;

    /** CosyVoice 专属音色 voice_id（由 /api/chat/voice/design 声音设计产生） */
    private String voiceId;

    /** 是否允许主动发消息 */
    private Boolean proactiveEnabled;

    /** 下次主动发言窗口开启时间（LLM 决策写入） */
    private Date nextProactiveAt;

    /** 上次主动发言时间（用于冷却） */
    private Date lastProactiveAt;

    /** 主动计数归属日（用于每日上限） */
    private Date proactiveDate;

    /** 当日主动发言次数 */
    private Integer proactiveCount;

    private Integer version;

    private Date createdAt;

    private Date updatedAt;
}
