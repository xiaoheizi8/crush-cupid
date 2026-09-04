package cn.yzfy.crushApp.api;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 全局 OkHttp 单例。 */
public final class Http {
    public static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Config.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Config.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(new AuthInterceptor())
            .build();

    private Http() {
    }

    static class AuthInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            String token = AuthApi.getToken();
            if (token != null && !token.isEmpty()) {
                Request.Builder builder = original.newBuilder()
                        .header("Authorization", token);
                return chain.proceed(builder.build());
            }
            return chain.proceed(original);
        }
    }
}