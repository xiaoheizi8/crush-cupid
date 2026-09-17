package cn.yzfy.crushApp.model;

/** 登录/注册成功响应（含 Sa-Token 会话） */
public class LoginVO {
    public String tokenName;
    public String tokenValue;
    public long expiresIn;
    public User user;
}