package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.dto.ChatRequest;
import cn.yzfy.crushApp.model.ChatHistory;

/** 对话：SSE 流式 / 历史 / 主动监听 */
public final class ChatApi {

    private ChatApi() {
    }

    public static Sse.Handle streamChat(String crushSlug, String message,
                                        List<ChatRequest.ChatMedia> media,
                                        String skillPrompt, boolean advisorMode, Sse.Listener l) {
        ChatRequest body = new ChatRequest(crushSlug, message);
        body.media = media;
        body.skillPrompt = skillPrompt;
        body.advisorMode = advisorMode;
        return Sse.open("POST", "/api/chat", body, l);
    }

    public static Sse.Handle streamAdvisor(String crushSlug, String message,
                                           String skillPrompt, Sse.Listener l) {
        ChatRequest body = new ChatRequest(crushSlug, message);
        body.skillPrompt = skillPrompt;
        body.advisorMode = true;
        return Sse.open("POST", "/api/chat/advisor", body, l);
    }

    public static Sse.Handle streamProactive(String crushSlug, String contextHint, Sse.Listener l) {
        cn.yzfy.crushApp.dto.ProactiveRequest body = new cn.yzfy.crushApp.dto.ProactiveRequest();
        body.crushSlug = crushSlug;
        body.contextHint = contextHint;
        return Sse.open("POST", "/api/chat/proactive", body, l);
    }

    public static void history(String crushSlug, Rest.Callback<List<ChatHistory>> cb) {
        Rest.get("/api/chat/history?crushSlug=" + encode(crushSlug),
                new TypeToken<Result<List<ChatHistory>>>() {
                }.getType(), cb);
    }

    public static Sse.Handle listenProactive(String crushSlug, Sse.Listener l) {
        return Sse.open("GET", "/api/push/listen?crushSlug=" + encode(crushSlug), null, l);
    }

    public static String encode(String s) {
        try {
            return java.net.URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}