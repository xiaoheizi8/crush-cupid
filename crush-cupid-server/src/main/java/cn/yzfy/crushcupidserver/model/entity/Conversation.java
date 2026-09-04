package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 对话记录
 *
 * @TableName conversation
 */
@Data
@TableName("conversation")
public class Conversation implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long crushId;

    /** 会话归属用户（当前对话用户 id；0=系统共享/演示桶） */
    private Long userId;

    /** user / assistant */
    private String role;

    private String content;

    private Date createdAt;
}
