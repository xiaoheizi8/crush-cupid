package cn.yzfy.crushcupidserver.mapper;

import cn.yzfy.crushcupidserver.model.entity.SysUsageDaily;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 针对表【sys_usage_daily】的数据库操作Mapper
 */
public interface SysUsageDailyMapper extends BaseMapper<SysUsageDaily> {

    /** 按日 upsert 并累加消息/对话计数（getMessageCount 递增） */
    @Insert("INSERT INTO sys_usage_daily(user_id, usage_date, message_count, chat_count, created_at, updated_at) " +
            "VALUES(#{userId}, #{date}, #{msg}, #{chat}, NOW(), NOW()) " +
            "ON CONFLICT (user_id, usage_date) DO UPDATE SET " +
            "message_count = sys_usage_daily.message_count + #{msg}, " +
            "chat_count = sys_usage_daily.chat_count + #{chat}, " +
            "updated_at = NOW()")
    int upsertChat(@Param("userId") Long userId,
                   @Param("date") LocalDate date,
                   @Param("msg") int msg,
                   @Param("chat") int chat);

    /** 按日 upsert 并累加 LLM 调用计数 */
    @Insert("INSERT INTO sys_usage_daily(user_id, usage_date, provider_calls, created_at, updated_at) " +
            "VALUES(#{userId}, #{date}, 1, NOW(), NOW()) " +
            "ON CONFLICT (user_id, usage_date) DO UPDATE SET " +
            "provider_calls = sys_usage_daily.provider_calls + 1, " +
            "updated_at = NOW()")
    int upsertProviderCall(@Param("userId") Long userId, @Param("date") LocalDate date);
}
