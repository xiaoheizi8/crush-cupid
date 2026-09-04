package cn.yzfy.crushcupidserver.service;

import cn.yzfy.crushcupidserver.model.entity.SysUsageDaily;
import com.baomidou.mybatisplus.spring.service.IService;

import java.time.LocalDate;

/**
 * 每日用量数据访问 Service
 */
public interface SysUsageDailyService extends IService<SysUsageDaily> {

    /** 今日用量；无则 null */
    SysUsageDaily getToday(Long userId);

    /** 当日消息/对话计数 +1（upsert） */
    void incrementChat(Long userId, int chatDelta, int messageDelta);

    /** 当日 LLM 调用 +1（upsert） */
    void incrementProviderCall(Long userId);

    /** 今日已用消息数 */
    int todayMessageCount(Long userId);
}
