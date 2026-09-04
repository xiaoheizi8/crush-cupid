package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户配额
 *
 * @TableName sys_quota
 */
@Data
@TableName("sys_quota")
public class SysQuota implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String plan;

    private Integer crushLimit;

    private Integer dailyChatLimit;

    /** JSON 数组字符串：允许的供应商 key 白名单 */
    private String modelAllowlist;

    private Date effectiveFrom;

    private Date effectiveTo;

    private Date createdAt;

    private Date updatedAt;
}
