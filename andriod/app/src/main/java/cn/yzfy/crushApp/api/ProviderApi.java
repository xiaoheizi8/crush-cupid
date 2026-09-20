package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import cn.yzfy.crushApp.model.AiProvider;

/** 自定义 LLM 供应商 CRUD */
public final class ProviderApi {

    private static final Type LIST = new TypeToken<Result<List<AiProvider>>>() {
    }.getType();
    private static final Type ONE = new TypeToken<Result<AiProvider>>() {
    }.getType();

    private ProviderApi() {
    }

    public static void list(Rest.Callback<List<AiProvider>> cb) {
        Rest.get("/api/provider", LIST, cb);
    }

    public static void create(AiProvider p, Rest.Callback<AiProvider> cb) {
        Rest.post("/api/provider", p, ONE, cb);
    }

    public static void update(long id, AiProvider p, Rest.Callback<AiProvider> cb) {
        Rest.put("/api/provider/" + id, p, ONE, cb);
    }

    public static void delete(long id, Rest.Callback<Void> cb) {
        Rest.delete("/api/provider/" + id, cb);
    }
}