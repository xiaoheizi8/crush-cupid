package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 每日用量（按 user_id + usage_date 唯一，日级 upsert）
 *
 * @TableName sys_usage_daily
 */
@Data
@TableName("sys_usage_daily")
public class SysUsageDaily implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate usageDate;

    private Integer chatCount;

    private Integer messageCount;

    private Integer providerCalls;

    private Date createdAt;

    private Date updatedAt;
}
