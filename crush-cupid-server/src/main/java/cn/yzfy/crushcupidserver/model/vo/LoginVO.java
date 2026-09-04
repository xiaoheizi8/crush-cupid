package cn.yzfy.crushcupidserver.model.vo;

import lombok.Data;

/**
 * 登录/注册成功视图（含 Sa-Token 会话）
 */
@Data
public class LoginVO {

    /** token 名称（请求头） */
    private String tokenName;

    /** token 值 */
    private String tokenValue;

    /** token 有效期（秒） */
    private long expiresIn;

    /** 用户信息 */
    private UserVO user;
}
