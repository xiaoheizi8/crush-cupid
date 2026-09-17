package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import cn.yzfy.crushApp.model.CaptchaVO;
import cn.yzfy.crushApp.model.LoginVO;
import cn.yzfy.crushApp.model.User;

/** 认证：验证码、登录 / 注册 / 登出 / 当前用户 */
public final class AuthApi {

    private AuthApi() {
    }

    /** 获取图形验证码（登录前置，可点击刷新） */
    public static void getCaptcha(Rest.Callback<CaptchaVO> cb) {
        Rest.get("/api/auth/captcha", new TypeToken<Result<CaptchaVO>>() {
        }.getType(), cb);
    }

    /** 发送邮箱验证码（purpose: REGISTER / LOGIN / RESET_PWD） */
    public static void sendEmailCode(String email, String purpose, Rest.Callback<Void> cb) {
        Rest.post("/api/auth/email-code", new EmailCodeRequest(email, purpose),
                new TypeToken<Result<Void>>() {
                }.getType(), cb);
    }

    /** 邮箱注册（注册即登录） */
    public static void register(String email, String password, String username, String code, Rest.Callback<LoginVO> cb) {
        Rest.post("/api/auth/register", new RegisterRequest(email, password, username, code),
                new TypeToken<Result<LoginVO>>() {
                }.getType(), cb);
    }

    /** 邮箱登录（前置图形验证码） */
    public static void login(String email, String password, String captchaId, String captcha, Rest.Callback<LoginVO> cb) {
        Rest.post("/api/auth/login", new LoginRequest(email, password, captchaId, captcha),
                new TypeToken<Result<LoginVO>>() {
                }.getType(), cb);
    }

    /** 登出 */
    public static void logout(Rest.Callback<Void> cb) {
        Rest.post("/api/auth/logout", null, new TypeToken<Result<Void>>() {
        }.getType(), cb);
    }

    /** 当前登录用户 */
    public static void me(Rest.Callback<User> cb) {
        Rest.get("/api/auth/me", new TypeToken<Result<User>>() {
        }.getType(), cb);
    }

    /** 保存 token 到 SharedPreferences */
    public static void saveToken(String tokenValue) {
        AppPrefs.putString("satoken", tokenValue);
    }

    /** 清除 token */
    public static void clearToken() {
        AppPrefs.remove("satoken");
    }

    /** 获取当前 token */
    public static String getToken() {
        return AppPrefs.getString("satoken", null);
    }

    /** 是否已登录 */
    public static boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }
}

/** 验证码请求 */
class EmailCodeRequest {
    public String email;
    public String purpose;

    EmailCodeRequest(String email, String purpose) {
        this.email = email;
        this.purpose = purpose;
    }
}

/** 登录请求 */
class LoginRequest {
    public String email;
    public String password;
    public String captchaId;
    public String captcha;

    LoginRequest(String email, String password, String captchaId, String captcha) {
        this.email = email;
        this.password = password;
        this.captchaId = captchaId;
        this.captcha = captcha;
    }
}

/** 注册请求 */
class RegisterRequest {
    public String email;
    public String password;
    public String username;
    public String code;

    RegisterRequest(String email, String password, String username, String code) {
        this.email = email;
        this.password = password;
        this.username = username;
        this.code = code;
    }
}