package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

/**
 * 我的用量/配额视图（用户中心）
 */
@Data
public class MyQuotaVO {

    private String plan;
    private Integer crushLimit;
    private Integer dailyChatLimit;
    /** 今日已用对话消息数 */
    private Integer todayMessageCount;
    /** 我的 crush 数 */
    private Long crushCount;
}
