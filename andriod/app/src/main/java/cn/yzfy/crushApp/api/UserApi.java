package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import cn.yzfy.crushApp.model.MyQuota;
import cn.yzfy.crushApp.model.User;

/** 用户中心：资料、改密码、配额 */
public final class UserApi {

    private UserApi() {
    }

    /** 当前用户资料（GET /api/user/me） */
    public static void me(Rest.Callback<User> cb) {
        Rest.get("/api/user/me", new TypeToken<Result<User>>() {
        }.getType(), cb);
    }

    /** 更新资料（用户名 / 头像），null 字段不修改 */
    public static void updateProfile(String username, String avatarUrl, Rest.Callback<User> cb) {
        Rest.put("/api/user/profile", new ProfileRequest(username, avatarUrl),
                new TypeToken<Result<User>>() {
                }.getType(), cb);
    }

    /**
     * 上传头像（multipart/form-data，POST /api/user/avatar）。
     * 服务端优先存储到第三方 OSS、失败回退本地磁盘，成功后返回最新资料（含新 avatarUrl）。
     */
    public static void uploadAvatar(byte[] bytes, String fileName, String mimeType, Rest.Callback<User> cb) {
        String safeMime = (mimeType != null && mimeType.startsWith("image/")) ? mimeType : "image/jpeg";
        MediaType mt = MediaType.parse(safeMime);
        if (mt == null) {
            mt = MediaType.parse("image/jpeg");
        }
        RequestBody body = RequestBody.create(bytes, mt);
        MultipartBody multipart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",
                        fileName == null || fileName.isEmpty() ? "avatar.jpg" : fileName, body)
                .build();
        Rest.upload("/api/user/avatar", multipart, new TypeToken<Result<User>>() {
        }.getType(), cb);
    }

    /** 我的配额（crush 上限 / 每日对话上限 / 今日已用 / 套餐） */
    public static void quota(Rest.Callback<MyQuota> cb) {
        Rest.get("/api/user/quota", new TypeToken<Result<MyQuota>>() {
        }.getType(), cb);
    }

    /** 修改密码（需验证旧密码） */
    public static void changePassword(String oldPassword, String newPassword, Rest.Callback<Void> cb) {
        Rest.post("/api/auth/password", new ChangePasswordRequest(oldPassword, newPassword),
                new TypeToken<Result<Void>>() {
                }.getType(), cb);
    }
}

/** 资料更新请求 */
class ProfileRequest {
    public String username;
    public String avatarUrl;

    ProfileRequest(String username, String avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
    }
}

/** 修改密码请求 */
class ChangePasswordRequest {
    public String oldPassword;
    public String newPassword;

    ChangePasswordRequest(String oldPassword, String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
