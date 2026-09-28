package cn.yzfy.crushApp.dto;

/** crush 创建/更新载荷：create 传 slug，update 忽略 slug */
public class CrushPayload {
    public String name;
    public String slug;
    public String mbti;
    public String zodiac;
    public String occupation;
    public String gender;
    public String knowDuration;
    public String relationshipStatus;
    public String impression;
    public String voiceId;
    /** 是否允许 ta 主动发消息（null = 不修改） */
    public Boolean proactiveEnabled;
}