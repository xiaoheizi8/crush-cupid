package cn.yzfy.crushcupidserver.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户聊天加密密钥（点对点/按用户）。
 * <p>每用户一行，存一个 32 字节随机 AES 密钥（经全局 KEK 包裹，见 keyEnc+keyNonce）。
 * 该用户名下 crush 的 conversation.content 用此用户密钥加解密，实现按用户隔离。
 *
 * @TableName sys_user_key
 */
@Data
@TableName("sys_user_key")
public class SysUserKey implements Serializable {

    /** 归属用户 id（>0 真实用户；user_id=0 共享桶不落此行） */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /** 用户 AES 密钥（加密方式是 base64 串）经全局 KEK 加密后的密文 */
    private byte[] keyEnc;

    /** 加密用户密钥所用 12B nonce */
    private byte[] keyNonce;

    private Date createdAt;

    private Date updatedAt;
}
