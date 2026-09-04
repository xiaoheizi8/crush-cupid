package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审计日志（只增不改，detail JSONB，不记聊天明文/Key 明文）
 *
 * @TableName sys_audit_log
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String email;

    private String module;

    private String action;

    private String resourceType;

    private String resourceId;

    private String detail;

    private String requestId;

    private String ip;

    private String userAgent;

    private String result;

    private String errorMessage;

    private Integer latencyMs;

    private Date createdAt;
}
