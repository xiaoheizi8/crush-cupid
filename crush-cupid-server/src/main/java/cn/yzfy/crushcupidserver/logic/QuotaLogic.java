package cn.yzfy.crushcupidserver.logic;

import cn.yzfy.crushcupidserver.exception.BizException;
import cn.yzfy.crushcupidserver.model.entity.SysQuota;
import cn.yzfy.crushcupidserver.service.SysConfigService;
import cn.yzfy.crushcupidserver.service.SysQuotaService;
import cn.yzfy.crushcupidserver.service.SysUsageDailyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 配额业务：默认配额初始化、每日对话限额、crush 数量限额、用量计数。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaLogic {

    private final SysQuotaService sysQuotaService;
    private final SysConfigService sysConfigService;
    private final SysUsageDailyService sysUsageDailyService;

    /** 读取用户配额；无则按系统默认创建并返回（幂等，一人一行） */
    @Transactional
    public SysQuota getOrCreateQuota(Long userId) {
        SysQuota quota = sysQuotaService.getByUserId(userId);
        if (quota != null) {
            return quota;
        }
        SysQuota q = new SysQuota();
        q.setUserId(userId);
        q.setPlan(sysConfigService.getValue("quota.default-plan", "free"));
        q.setCrushLimit(Math.max(1, intOf(sysConfigService.getValue("quota.default-crush-limit", "10"))));
        q.setDailyChatLimit(Math.max(1, intOf(sysConfigService.getValue("quota.default-daily-chat-limit", "100"))));
        q.setCreatedAt(new Date());
        q.setUpdatedAt(new Date());
        try {
            sysQuotaService.save(q);
            return q;
        } catch (Exception e) {
            // 并发首次创建时撞唯一索引 idx_quota_user(user_id)：回读既有行，保证一人一行
            SysQuota existing = sysQuotaService.getByUserId(userId);
            if (existing != null) {
                return existing;
            }
            throw e;
        }
    }

    /** 对话前校验今日消息限额（超出抛 429 语义的业务异常） */
    public void checkDailyChatLimit(Long userId) {
        SysQuota quota = getOrCreateQuota(userId);
        int limit = quota.getDailyChatLimit() == null ? Integer.MAX_VALUE : quota.getDailyChatLimit();
        int used = sysUsageDailyService.todayMessageCount(userId);
        if (used >= limit) {
            throw new BizException(429, "今日对话次数已达上限（" + limit + "），请明天再试或升级套餐");
        }
    }

    /** crush 数量是否已达上限（创建时校验） */
    public void checkCrushLimit(Long userId, long currentCount) {
        SysQuota quota = getOrCreateQuota(userId);
        int limit = quota.getCrushLimit() == null ? Integer.MAX_VALUE : quota.getCrushLimit();
        if (currentCount >= limit) {
            throw new BizException(429, "crush 数量已达上限（" + limit + "），请删除后再创建或升级套餐");
        }
    }

    /** 今日已用消息数 */
    public int todayMessages(Long userId) {
        return sysUsageDailyService.todayMessageCount(userId);
    }

    /** 记录一次对话产出消息（增量统计，不给业务负担） */
    public void consumeMessages(Long userId, int chatDelta, int messageDelta) {
        try {
            sysUsageDailyService.incrementChat(userId, chatDelta, messageDelta);
        } catch (Exception e) {
            log.warn("用量统计失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 记录一次 LLM 供应商调用（用量报表/配额） */
    public void consumeProviderCall(Long userId) {
        try {
            sysUsageDailyService.incrementProviderCall(userId);
        } catch (Exception e) {
            log.warn("用量统计(provider)失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private int intOf(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 100;
        }
    }
}
