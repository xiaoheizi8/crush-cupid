package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.model.Source;

/**
 * 素材导入组件：类型选择 + 文本粘贴 + 图片（视觉/OCR）+ 文件上传 + 素材列表行渲染 + 删除。
 * <p>
 * CrushDetailFragment 与 CrushEditFragment 共用，消除两处近乎逐行相同的实现。
 * 素材类型与后端 {@code SourceType} 枚举对齐：TEXT / WECHAT / QQ / SOCIAL / PHOTO。
 */
public final class SourceImporter {

    /** 素材类型：{value, 标签}，与后端 SourceImportDTO.type 对齐 */
    public static final String[][] TYPES = {
            {"TEXT", "📝 普通"},
            {"WECHAT", "💬 微信"},
            {"QQ", "🐧 QQ"},
            {"SOCIAL", "🌐 社交"},
            {"PHOTO", "📷 照片"},
    };

    /** 导入/删除成功后的回调（宿主用于刷新列表） */
    public interface Callback {
        void onChanged();
    }

    private final Fragment host;
    private final long crushId;
    private final Callback callback;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker;
    private final ActivityResultLauncher<String> filePicker;

    /** 对话框内当前选中的素材类型 */
    private String selectedType = "TEXT";

    /** @param host 宿主 Fragment（用于注册选图/选文件 Launcher，须在 Fragment CREATED 阶段构造） */
    public SourceImporter(Fragment host, long crushId, Callback callback) {
        this.host = host;
        this.crushId = crushId;
        this.callback = callback;
        photoPicker = host.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) uploadUri(uri, true);
                });
        filePicker = host.registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) uploadUri(uri, false);
                });
    }

    /** 弹出导入对话框：类型 chips + 文本输入 + 图片/文件入口 */
    public void showDialog() {
        android.content.Context ctx = host.requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18));

        TextView tip = new TextView(ctx);
        tip.setText("选择素材类型，粘贴 TA 说过的话或聊天记录；也可上传截图（视觉理解）或 txt/json/html/csv 导出文件。");
        tip.setTextSize(12);
        tip.setTextColor(0xFFA5929C);
        box.addView(tip);

        // 类型选择 chips（横滑）
        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(ctx);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout typeRow = new LinearLayout(ctx);
        typeRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(ctx, 10);
        scroll.setLayoutParams(tlp);
        scroll.addView(typeRow);
        selectedType = "TEXT";
        for (String[] t : TYPES) {
            typeRow.addView(typeChip(ctx, t[0], t[1]));
        }
        box.addView(scroll);

        EditText content = new EditText(ctx);
        content.setHint("内容…");
        content.setTextSize(14);
        content.setMinLines(3);
        content.setBackground(Ui.rounded(0xFFFBF6F7, 10));
        content.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(ctx, 8);
        content.setLayoutParams(clp);
        box.addView(content);

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(ctx, 12);
        btns.setLayoutParams(blp);

        TextView photo = plainBtn(ctx, "📷 图片");
        photo.setOnClickListener(v -> {
            d.dismiss();
            photoPicker.launch(new PickVisualMediaRequest.Builder().build());
        });
        btns.addView(photo);

        TextView file = plainBtn(ctx, "📄 文件");
        file.setOnClickListener(v -> {
            d.dismiss();
            filePicker.launch("*/*");
        });
        btns.addView(file);

        TextView cancel = plainBtn(ctx, "取消");
        cancel.setTextColor(0xFFA5929C);
        cancel.setOnClickListener(v -> d.dismiss());
        btns.addView(cancel);

        TextView ok = new TextView(ctx);
        ok.setText("导入");
        ok.setTextSize(14);
        ok.setTextColor(0xFFFFFFFF);
        ok.setBackground(Ui.rounded(0xFFFF5A7A, 10));
        ok.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 6), Ui.dp(ctx, 14), Ui.dp(ctx, 6));
        ok.setOnClickListener(v -> {
            String text = content.getText().toString().trim();
            if (text.isEmpty()) {
                Ui.toast(ctx, "内容不能为空", FriendlyToast.Type.WARN);
                return;
            }
            d.dismiss();
            importText(text, selectedType);
        });
        btns.addView(ok);
        box.addView(btns);

        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    private TextView typeChip(android.content.Context ctx, String value, String label) {
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(ctx, 6);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> {
            selectedType = value;
            LinearLayout parent = (LinearLayout) t.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                TextView sib = (TextView) parent.getChildAt(i);
                sib.setTextColor(0xFF9B5A66);
                sib.setBackground(Ui.rounded(0xFFF6EEF1, 999));
            }
            t.setTextColor(0xFFFFFFFF);
            t.setBackground(Ui.rounded(0xFFFF5A7A, 999));
        });
        // 默认选中第一个
        if ("TEXT".equals(value)) {
            t.setTextColor(0xFFFFFFFF);
            t.setBackground(Ui.rounded(0xFFFF5A7A, 999));
        } else {
            t.setTextColor(0xFF9B5A66);
            t.setBackground(Ui.rounded(0xFFF6EEF1, 999));
        }
        Ui.pressScale(t);
        return t;
    }

    private TextView plainBtn(android.content.Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(0xFF6B5E70);
        t.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        return t;
    }

    /** 文本素材导入（带类型） */
    public void importText(String text, String type) {
        android.content.Context ctx = host.requireContext();
        CrushApi.addSource(crushId, text, type, null, new Rest.Callback<Source>() {
            @Override
            public void ok(Source data) {
                Ui.toast(ctx, "已导入，AI 正在理解 ♥", FriendlyToast.Type.SUCCESS);
                if (callback != null) callback.onChanged();
            }

            @Override
            public void fail(String message) {
                Ui.toast(ctx, message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    /** 图片/文件上传（类型由后端按扩展名推断：图片→PHOTO，json/html/csv→WECHAT） */
    public void uploadUri(Uri uri, boolean image) {
        android.content.Context ctx = host.requireContext();
        try {
            InputStream is = ctx.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            is.close();
            String mime = ctx.getContentResolver().getType(uri);
            if (mime == null) mime = image ? "image/jpeg" : "application/octet-stream";
            // 尽量带上原始文件名（后端靠扩展名推断类型），拿不到就用时间戳兜底
            String name = queryFileName(ctx, uri);
            if (TextUtils.isEmpty(name)) {
                String ext = mime.contains("jpeg") || mime.contains("jpg") ? ".jpg"
                        : mime.contains("png") ? ".png" : "";
                name = System.currentTimeMillis() + ext;
            }
            upload(bos.toByteArray(), name, mime);
        } catch (Exception e) {
            Ui.toast(ctx, image ? "读取图片失败，请换一张重试" : "读取文件失败，请重试",
                    FriendlyToast.Type.ERROR);
        }
    }

    private String queryFileName(android.content.Context ctx, Uri uri) {
        try (android.database.Cursor c = ctx.getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    String name = c.getString(idx);
                    if (!TextUtils.isEmpty(name)) return name;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void upload(byte[] bytes, String name, String mime) {
        android.content.Context ctx = host.requireContext();
        final Dialog loading = Ui.loading(ctx, "上传并理解中…");
        CrushApi.uploadSource(crushId, bytes, name, mime, new Rest.Callback<Source>() {
            @Override
            public void ok(Source data) {
                Ui.dismiss(loading);
                Ui.toast(ctx, "已导入并理解 ♥", FriendlyToast.Type.SUCCESS);
                if (callback != null) callback.onChanged();
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(loading);
                Ui.toast(ctx, message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    /** 删除素材（确认后） */
    public void delete(Source s) {
        android.content.Context ctx = host.requireContext();
        Ui.confirm(ctx, "删除", "删除这条原材料？", "删除", () ->
                CrushApi.deleteSource(crushId, s.id, new Rest.Callback<Void>() {
                    @Override
                    public void ok(Void data) {
                        if (callback != null) callback.onChanged();
                    }

                    @Override
                    public void fail(String message) {
                        Ui.toast(ctx, message, FriendlyToast.Type.ERROR);
                    }
                }));
    }

    /**
     * 渲染一条素材卡片：类型 chip + 文件名 + 删除 + 内容预览 + AI 分析摘要。
     * CrushEdit 与 CrushDetail 的素材列表共用。
     */
    public View sourceRow(Source s) {
        android.content.Context ctx = host.requireContext();
        LinearLayout row = Ui.card(ctx, 14);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = Ui.dp(ctx, 5);
        row.setLayoutParams(rp);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(top);

        TextView type = Ui.chip(ctx, s.type == null ? "TEXT" : s.type);
        top.addView(type);
        TextView name = new TextView(ctx);
        name.setText(TextUtils.isEmpty(s.fileName) ? "文本材料" : s.fileName);
        name.setTextSize(13);
        name.setTextColor(0xFF4A4052);
        name.setPadding(Ui.dp(ctx, 8), 0, 0, 0);
        top.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView del = new TextView(ctx);
        del.setText("删除");
        del.setTextSize(12);
        del.setTextColor(0xFFFF4D6A);
        del.setOnClickListener(v -> delete(s));
        top.addView(del);

        if (!TextUtils.isEmpty(s.content)) {
            TextView preview = new TextView(ctx);
            String c = s.content.length() > 60 ? s.content.substring(0, 60) + "…" : s.content;
            preview.setText(c);
            preview.setTextSize(12);
            preview.setTextColor(0xFFA5929C);
            preview.setMaxLines(2);
            preview.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(preview);
        }
        if (!TextUtils.isEmpty(s.analysis)) {
            TextView analyze = new TextView(ctx);
            String a = readAnalysis(s.analysis);
            a = a.length() > 90 ? a.substring(0, 90) + "…" : a;
            analyze.setText("✨ " + a);
            analyze.setTextSize(12);
            analyze.setTextColor(0xFF7256FF);
            analyze.setMaxLines(3);
            analyze.setEllipsize(TextUtils.TruncateAt.END);
            analyze.setPadding(0, Ui.dp(ctx, 4), 0, 0);
            row.addView(analyze);
        }
        return row;
    }

    /** 解析 LLM 分析 JSON（keyPoints / raw），失败回落原文 */
    public static String readAnalysis(String json) {
        if (TextUtils.isEmpty(json)) {
            return "";
        }
        try {
            String t = json.indexOf('{') >= 0 && json.lastIndexOf('}') > json.indexOf('{')
                    ? json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1) : json;
            com.google.gson.JsonObject o = GsonFactory.GSON
                    .fromJson(t, com.google.gson.JsonObject.class);
            if (o != null) {
                String kp = o.has("keyPoints") && !o.get("keyPoints").isJsonNull()
                        ? o.get("keyPoints").getAsString() : "";
                if (!TextUtils.isEmpty(kp)) {
                    return "关键要点：" + kp;
                }
                String raw = o.has("raw") && !o.get("raw").isJsonNull()
                        ? o.get("raw").getAsString() : "";
                if (!TextUtils.isEmpty(raw)) {
                    return "已记录（原文本）";
                }
            }
        } catch (Exception ignored) {
        }
        return json.replace('\n', ' ');
    }
}
