package cn.yzfy.crushApp.api;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** REST 请求封装：OkHttp + Result 解包，回调回主线程。 */
public final class Rest {
    public interface Callback<T> {
        void ok(T data);

        void fail(String message);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Rest() {
    }

    public static <T> void get(String path, Type type, Callback<T> cb) {
        req("GET", path, null, type, cb);
    }

    public static <T> void post(String path, Object body, Type type, Callback<T> cb) {
        req("POST", path, body, type, cb);
    }

    public static <T> void put(String path, Object body, Type type, Callback<T> cb) {
        req("PUT", path, body, type, cb);
    }

    public static void delete(String path, Callback<Void> cb) {
        req("DELETE", path, null, new com.google.gson.reflect.TypeToken<Result<Void>>() {
        }.getType(), (Callback<Void>) cb);
    }

    /** multipart 上传 */
    public static <T> void upload(String path, okhttp3.MultipartBody multipart, Type type, Callback<T> cb) {
        Request request = new Request.Builder().url(Config.BASE_URL + path).post(multipart).build();
        send(request, type, cb);
    }

    private static <T> void send(Request request, final Type type, final Callback<T> cb) {
        Http.client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                try {
                    String text = response.body() == null ? "" : response.body().string();
                    if (!response.isSuccessful()) {
                        final int code = response.code();
                        final String msg = extractMsg(text, "请求失败 HTTP " + code);
                        main(() -> {
                            if (Session.isUnauthorized(code)) {
                                Session.handleUnauthorized();
                            }
                            cb.fail(msg);
                        });
                        return;
                    }
                    final Result<T> r = GsonFactory.GSON.fromJson(text, type);
                    main(() -> {
                        if (r != null && r.ok()) {
                            cb.ok(r.data);
                        } else {
                            if (r != null && Session.isUnauthorized(r.code)) {
                                Session.handleUnauthorized();
                            }
                            cb.fail(r != null && r.message != null && !r.message.isEmpty() ? r.message : "响应异常");
                        }
                    });
                } catch (Exception e) {
                    main(() -> cb.fail("响应解析失败，请稍后再试"));
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                main(() -> cb.fail("网络连接失败，请检查网络后重试"));
            }
        });
    }

    private static <T> void req(final String method, String path, Object body, final Type type, final Callback<T> cb) {
        Request.Builder rb = new Request.Builder().url(Config.BASE_URL + path);
        if ("POST".equals(method) || "PUT".equals(method)) {
            String json = body == null ? "" : GsonFactory.GSON.toJson(body);
            rb.method(method, RequestBody.create(json, JSON));
        } else {
            rb.method(method, null);
        }
        send(rb.build(), type, cb);
    }

    static String extractMsg(String bodyJson, String fallback) {
        if (bodyJson == null) {
            return fallback;
        }
        try {
            Result<?> r = GsonFactory.GSON.fromJson(bodyJson, Result.class);
            if (r != null && r.message != null && !r.message.isEmpty()) {
                return r.message;
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    static void main(final Runnable r) {
        MAIN.post(r);
    }
}