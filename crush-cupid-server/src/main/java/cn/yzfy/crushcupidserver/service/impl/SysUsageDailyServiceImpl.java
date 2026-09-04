package cn.yzfy.crushcupidserver.service.impl;

import cn.yzfy.crushcupidserver.mapper.SysUsageDailyMapper;
import cn.yzfy.crushcupidserver.model.entity.SysUsageDaily;
import cn.yzfy.crushcupidserver.service.SysUsageDailyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 每日用量 Service 实现
 */
@Service
public class SysUsageDailyServiceImpl extends ServiceImpl<SysUsageDailyMapper, SysUsageDaily>
        implements SysUsageDailyService {

    @Override
    public SysUsageDaily getToday(Long userId) {
        return getOne(new LambdaQueryWrapper<SysUsageDaily>()
                .eq(SysUsageDaily::getUserId, userId)
                .eq(SysUsageDaily::getUsageDate, LocalDate.now())
                .last("LIMIT 1"));
    }

    @Override
    public void incrementChat(Long userId, int chatDelta, int messageDelta) {
        baseMapper.upsertChat(userId, LocalDate.now(), messageDelta, chatDelta);
    }

    @Override
    public void incrementProviderCall(Long userId) {
        baseMapper.upsertProviderCall(userId, LocalDate.now());
    }

    @Override
    public int todayMessageCount(Long userId) {
        SysUsageDaily today = getToday(userId);
        return today == null || today.getMessageCount() == null ? 0 : today.getMessageCount();
    }
}
