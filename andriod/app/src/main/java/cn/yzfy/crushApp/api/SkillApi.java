package cn.yzfy.crushApp.api;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import cn.yzfy.crushApp.model.AdvisorCommand;
import cn.yzfy.crushApp.model.CrushReport;
import cn.yzfy.crushApp.model.SkillCatalog;

/** Skill 目录 / 军师子命令 / 关系报告 */
public final class SkillApi {

    private static final Type CATALOG = new TypeToken<Result<SkillCatalog>>() {
    }.getType();
    private static final Type COMMANDS = new TypeToken<Result<List<AdvisorCommand>>>() {
    }.getType();
    private static final Type STRING_T = new TypeToken<Result<String>>() {
    }.getType();
    private static final Type REPORT = new TypeToken<Result<CrushReport>>() {
    }.getType();
    private static final Type REPORTS = new TypeToken<Result<List<CrushReport>>>() {
    }.getType();

    private SkillApi() {
    }

    public static void catalog(Rest.Callback<SkillCatalog> cb) {
        Rest.get("/api/skill/catalog", CATALOG, cb);
    }

    public static void prompt(String name, Rest.Callback<String> cb) {
        Rest.get("/api/skill/prompt/" + ChatApi.encode(name), STRING_T, cb);
    }

    public static void advisorCommands(Rest.Callback<List<AdvisorCommand>> cb) {
        Rest.get("/api/skill/advisor", COMMANDS, cb);
    }

    public static void invoke(String name, String question, String crushSlug, Rest.Callback<String> cb) {
        cn.yzfy.crushApp.dto.AdvisorInvoke body = new cn.yzfy.crushApp.dto.AdvisorInvoke();
        body.name = name;
        body.question = question;
        body.crushSlug = crushSlug;
        Rest.post("/api/skill/advisor/invoke", body, STRING_T, cb);
    }

    public static void generateReport(String crushSlug, Rest.Callback<CrushReport> cb) {
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("crushSlug", crushSlug);
        Rest.post("/api/skill/advisor/report", body, REPORT, cb);
    }

    public static void reports(String crushSlug, Rest.Callback<List<CrushReport>> cb) {
        Rest.get("/api/skill/report/list?crushSlug=" + ChatApi.encode(crushSlug), REPORTS, cb);
    }

    public static void reportDetail(long id, Rest.Callback<CrushReport> cb) {
        Rest.get("/api/skill/report/" + id, REPORT, cb);
    }

    public static void deleteReport(long id, Rest.Callback<Void> cb) {
        Rest.delete("/api/skill/report/" + id, cb);
    }

    /** 报告导出回调：ok 在主线程返回已保存的文件 */
    public interface FileCallback {
        void ok(java.io.File file);

        void fail(String message);
    }

    /**
     * 下载已保存报告 .docx 到应用外部文档目录（免存储权限），完成后主线程回调。
     * 文件名：{标题（非法字符已清洗）}_{id}.docx
     */
    public static void downloadReport(android.content.Context ctx, long id, String title, FileCallback cb) {
        java.io.File dir = ctx.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) {
            dir = ctx.getFilesDir();
        }
        String safe = title == null ? "" : title.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (safe.length() > 40) {
            safe = safe.substring(0, 40);
        }
        final java.io.File out = new java.io.File(dir, (safe.isEmpty() ? "关系报告" : safe) + "_" + id + ".docx");
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(cn.yzfy.crushApp.api.Config.BASE_URL + "/api/skill/report/" + id + "/download")
                .build();
        Http.client.newCall(req).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, java.io.IOException e) {
                Rest.main(() -> cb.fail("网络连接失败，请检查网络后重试"));
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response resp) {
                try (okhttp3.ResponseBody body = resp.body()) {
                    if (!resp.isSuccessful() || body == null) {
                        Rest.main(() -> cb.fail("下载失败 HTTP " + resp.code()));
                        return;
                    }
                    java.io.InputStream is = body.byteStream();
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(out);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) {
                        fos.write(buf, 0, n);
                    }
                    fos.close();
                    is.close();
                    Rest.main(() -> cb.ok(out));
                } catch (Exception e) {
                    Rest.main(() -> cb.fail("保存文件失败"));
                }
            }
        });
    }
}