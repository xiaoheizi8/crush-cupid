package cn.yzfy.crushApp.api;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSource;

/** SSE 流式读取：解析 data: 行，回调回主线程。 */
public final class Sse {
    public interface Listener {
        void onEvent(String data);

        void onClosed();

        void onError(String message);
    }

    public interface Handle {
        void close();
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Sse() {
    }

    public static Handle open(final String method, String path, Object body, final Listener l) {
        Request.Builder rb = new Request.Builder().url(Config.BASE_URL + path)
                .header("Accept", "text/event-stream");
        if ("POST".equals(method)) {
            rb.post(RequestBody.create(GsonFactory.GSON.toJson(body), JSON));
        } else {
            rb.get();
        }
        final Call call = Http.client.newCall(rb.build());
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onResponse(Call c, Response response) {
                if (!response.isSuccessful() || !hasSse(response)) {
                    String text = "";
                    try {
                        text = response.body() == null ? "" : response.body().string();
                    } catch (IOException ignored) {
                    }
                    final String errText = text;
                    final int code = response.code();
                    main(() -> {
                        if (Session.isUnauthorized(code)) {
                            Session.handleUnauthorized();
                        }
                        l.onError(Rest.extractMsg(errText, "请求失败 HTTP " + code));
                    });
                    return;
                }
                try (BufferedSource src = response.body().source()) {
                    while (!src.exhausted()) {
                        String line = src.readUtf8Line();
                        if (line == null) {
                            break;
                        }
                        if (line.startsWith("data:")) {
                            final String payload = line.substring(5).trim();
                            if (!payload.isEmpty()) {
                                main(() -> l.onEvent(payload));
                            }
                        }
                    }
                    main(l::onClosed);
                } catch (Exception e) {
                    if (!c.isCanceled()) {
                        main(() -> l.onError("流式连接中断"));
                    }
                }
            }

            @Override
            public void onFailure(Call c, IOException e) {
                if (c.isCanceled()) {
                    main(l::onClosed);
                } else {
                    main(() -> l.onError("网络连接失败，请检查网络后重试"));
                }
            }
        });
        return call::cancel;
    }

    private static boolean hasSse(Response resp) {
        String ct = resp.header("Content-Type", "");
        return ct.contains("text/event-stream");
    }

    static void main(final Runnable r) {
        MAIN.post(r);
    }
}