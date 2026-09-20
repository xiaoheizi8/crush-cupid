package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.Sse;
import cn.yzfy.crushApp.model.BuildEvent;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.Source;
import cn.yzfy.crushApp.model.Version;

/** 暗恋对象资料页：记忆 / 构建 / 原材料 / 版本 */
public class CrushDetailFragment extends Fragment {

    private Crush crush;
    private LinearLayout col;
    private TextView buildText;
    private LinearLayout sourcesBox;
    private LinearLayout versionsBox;
    private ActivityResultLauncher<PickVisualMediaRequest> picker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFBF3F5);

        // 头部
        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setBackgroundColor(0xFFFFFFFF);
        header.setElevation(Ui.dp(ctx, 2));
        header.setPadding(Ui.dp(ctx, 6), Ui.dp(ctx, 8), Ui.dp(ctx, 6), Ui.dp(ctx, 8));
        TextView back = new TextView(ctx);
        back.setText("‹");
        back.setTextSize(32);
        back.setTextColor(0xFF4A4052);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        Ui.pressScale(back);
        header.addView(back, Ui.dp(ctx, 44), Ui.dp(ctx, 44));
        TextView title = new TextView(ctx);
        title.setText(crush == null ? "资料" : crush.name);
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView edit = new TextView(ctx);
        edit.setText("编辑");
        edit.setTextSize(14);
        edit.setTextColor(0xFFFF5A7A);
        edit.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        edit.setOnClickListener(v -> Nav.push(requireActivity(), CrushEditFragment.class,
                HomeFragment.crushArgs(crush)));
        header.addView(edit);
        root.addView(header);

        // 滚动内容
        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 24));
        sv.addView(col);

        buildProfile(ctx);
        buildMemory(ctx);
        buildActions(ctx);
        buildText = new TextView(ctx);
        buildText.setTextSize(12);
        buildText.setTextColor(0xFF7256FF);
        buildText.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        col.addView(buildText);

        Ui.section(ctx, "原材料");
        sourcesBox = new LinearLayout(ctx);
        sourcesBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(sourcesBox);
        addSourceRow(ctx);

        Ui.section(ctx, "版本历史");
        versionsBox = new LinearLayout(ctx);
        versionsBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(versionsBox);

        Ui.enter(col, R.anim.fade_scale_in);

        return root;
    }

    private void buildProfile(android.content.Context ctx) {
        LinearLayout card = Ui.card(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        TextView avatar = Ui.avatar(ctx, crush == null ? "?" : crush.initial(), 0xFFFF5A7A, 56);
        card.addView(avatar);
        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(Ui.dp(ctx, 12), 0, 0, 0);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView name = new TextView(ctx);
        name.setText(crush == null ? "" : crush.name);
        name.setTextSize(19);
        name.setTextColor(0xFF2A2233);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        txt.addView(name);
        TextView stats = new TextView(ctx);
        stats.setTextSize(12);
        stats.setTextColor(0xFFA5929C);
        StringBuilder s = new StringBuilder();
        if (crush != null) {
            if (crush.stageLabel() != null && !crush.stageLabel().isEmpty()) s.append(crush.stageLabel());
            if (crush.currentStage != null) s.append(s.length() > 0 ? " · " : "").append("第").append(crush.currentStage).append("阶段");
            if (crush.totalMessages != null) s.append(s.length() > 0 ? " · " : "").append(crush.totalMessages).append(" 条消息");
        }
        stats.setText(s.length() == 0 ? "还没开始" : s.toString());
        txt.addView(stats);
        card.addView(txt);
        col.addView(card);

        // 信息 chips
        LinearLayout chips = new LinearLayout(ctx);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        int margin = Ui.dp(ctx, 2);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(ctx, 10);
        clp.setMargins(0, margin, 0, margin);
        chips.setLayoutParams(clp);
        col.addView(chips);
        if (crush != null) {
            addChip(chips, crush.mbti);
            addChip(chips, crush.zodiac);
            addChip(chips, crush.occupation);
            addChip(chips, crush.gender);
            addChip(chips, crush.knowDuration);
            addChip(chips, crush.relationshipStatus);
        }
    }

    private void addChip(LinearLayout parent, String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        TextView t = Ui.chip(parent.getContext(), text);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(parent.getContext(), 6);
        t.setLayoutParams(lp);
        parent.addView(t);
    }

    private void buildMemory(android.content.Context ctx) {
        Ui.section(ctx, "记忆");
        LinearLayout card = Ui.card(ctx);
        String[] labels = {"关系总览", "时间线", "甜蜜时刻", "互动模式"};
        String[] values = {crush == null ? null : crush.memoryOverview,
                crush == null ? null : crush.memoryTimeline,
                crush == null ? null : crush.memorySweet,
                crush == null ? null : crush.memoryInteraction};
        boolean any = false;
        for (int i = 0; i < 4; i++) {
            if (TextUtils.isEmpty(values[i])) {
                continue;
            }
            any = true;
            TextView head = new TextView(ctx);
            head.setText(labels[i]);
            head.setTextSize(13);
            head.setTextColor(0xFFE8405F);
            head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            hlp.topMargin = i == 0 ? 0 : Ui.dp(ctx, 10);
            head.setLayoutParams(hlp);
            card.addView(head);
            TextView body = new TextView(ctx);
            body.setText(values[i]);
            body.setTextSize(14);
            body.setTextColor(0xFF4A4052);
            body.setLineSpacing(Ui.dp(ctx, 2), 1f);
            card.addView(body);
        }
        if (!any) {
            TextView empty = new TextView(ctx);
            empty.setText("还没有记忆。去聊聊天，或导入原材料后点「重建人格」。");
            empty.setTextSize(13);
            empty.setTextColor(0xFFA5929C);
            empty.setPadding(0, Ui.dp(ctx, 4), 0, Ui.dp(ctx, 4));
            card.addView(empty);
        }
        col.addView(card);
    }

    private void buildActions(android.content.Context ctx) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(ctx, 14);
        row.setLayoutParams(rlp);
        col.addView(row);

        row.addView(actionBtn(ctx, "重建人格", 0xFFFF5A7A, () -> build()));
        row.addView(actionBtn(ctx, "去聊天", 0xFF7256FF, () ->
                Nav.push(requireActivity(), ChatFragment.class, HomeFragment.crushArgs(crush))));
        row.addView(actionBtn(ctx, "关系报告", 0xFF2FBF71, () ->
                Nav.push(requireActivity(), ReportsFragment.class, HomeFragment.crushArgs(crush))));
    }

    private TextView actionBtn(android.content.Context ctx, String text, int color, Runnable r) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.rounded(color, 12));
        t.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 9), Ui.dp(ctx, 12), Ui.dp(ctx, 9));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Ui.dp(ctx, 6);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> r.run());
        Ui.pressScale(t);
        return t;
    }

    private void addSourceRow(android.content.Context ctx) {
        TextView add = new TextView(ctx);
        add.setText("＋ 添加原材料");
        add.setTextSize(13);
        add.setTextColor(0xFFFF5A7A);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.rounded(0xFFFFF0F3, 12));
        add.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 9), Ui.dp(ctx, 12), Ui.dp(ctx, 9));
        add.setOnClickListener(v -> addSourceDialog());
        sourcesBox.addView(add);
    }

    private void addSourceDialog() {
        android.content.Context ctx = requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18));

        TextView tip = new TextView(ctx);
        tip.setText("粘贴一段 TA 说过的话、聊天记录或照片（OCR）。可点击右侧 📷 直接上传图片。");
        tip.setTextSize(12);
        tip.setTextColor(0xFFA5929C);
        box.addView(tip);

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

        TextView photo = new TextView(ctx);
        photo.setText("📷 图片");
        photo.setTextSize(14);
        photo.setTextColor(0xFF6B5E70);
        photo.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        photo.setOnClickListener(v -> {
            d.dismiss();
            pickPhoto();
        });
        btns.addView(photo);

        TextView cancel = new TextView(ctx);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setTextColor(0xFFA5929C);
        cancel.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
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
            importText(text);
        });
        btns.addView(ok);
        box.addView(btns);

        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    private void pickPhoto() {
        picker.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void importText(String text) {
        CrushApi.addSource(crush.id, text, "TEXT", null, new Rest.Callback<Source>() {
            @Override
            public void ok(Source data) {
                Ui.toast(requireContext(), "已导入", FriendlyToast.Type.SUCCESS);
                loadSources();
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private void upload(byte[] bytes, String name, String mime) {
        CrushApi.uploadSource(crush.id, bytes, name, mime, new Rest.Callback<Source>() {
            @Override
            public void ok(Source data) {
                Ui.toast(requireContext(), "图片已导入（OCR 或原图）", FriendlyToast.Type.SUCCESS);
                loadSources();
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private void build() {
        buildText.setText("开始构建…");
        CrushApi.build(crush.id, new Sse.Listener() {
            @Override
            public void onEvent(String data) {
                try {
                    BuildEvent ev = GsonFactory.GSON.fromJson(data, BuildEvent.class);
                    if ("progress".equals(ev.type)) {
                        buildText.setText("构建中：" + ev.message);
                    } else if ("done".equals(ev.type)) {
                        String v = ev.result != null && ev.result.version != null ? " v" + ev.result.version : "";
                        buildText.setText("✓ 构建完成" + v);
                        Ui.toast(requireContext(), "人格构建完成" + v, FriendlyToast.Type.SUCCESS);
                        Ui.post(() -> {
                            loadCrush();
                            loadVersions();
                        });
                    } else if ("error".equals(ev.type)) {
                        buildText.setTextColor(0xFFFF4D6A);
                        buildText.setText("构建失败：" + ev.message);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onClosed() {
            }

            @Override
            public void onError(String message) {
                buildText.setTextColor(0xFFFF4D6A);
                buildText.setText("构建失败：" + message);
            }
        });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        picker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri == null) {
                        return;
                    }
                    handlePhoto(uri);
                });
        loadCrush();
        loadSources();
        loadVersions();
    }

    private void handlePhoto(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            is.close();
            String mime = requireContext().getContentResolver().getType(uri);
            if (mime == null) mime = "image/jpeg";
            upload(bos.toByteArray(), System.currentTimeMillis() + ".jpg", mime);
        } catch (Exception e) {
            Ui.toast(requireContext(), "读取图片失败，请换一张重试", FriendlyToast.Type.ERROR);
        }
    }

    private void loadCrush() {
        CrushApi.get(crush.id, new Rest.Callback<Crush>() {
            @Override
            public void ok(Crush data) {
                crush = data;
                // 简单起见：重建当前 Fragment 视图以刷新记忆
                if (getView() != null) {
                    getParentFragmentManager().beginTransaction()
                            .detach(CrushDetailFragment.this)
                            .attach(CrushDetailFragment.this)
                            .commit();
                }
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private void loadSources() {
        CrushApi.listSources(crush.id, new Rest.Callback<List<Source>>() {
            @Override
            public void ok(List<Source> data) {
                // 移除动态添加的行（第一行是「+添加」，保留之）
                while (sourcesBox.getChildCount() > 1) {
                    sourcesBox.removeViewAt(1);
                }
                if (data != null) {
                    for (final Source s : data) {
                        sourcesBox.addView(sourceRow(s));
                    }
                }
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private View sourceRow(final Source s) {
        android.content.Context ctx = requireContext();
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
        del.setOnClickListener(v -> Ui.confirm(ctx, "删除", "删除这条原材料？", "删除", () ->
                CrushApi.deleteSource(crush.id, s.id, new Rest.Callback<Void>() {
                    @Override
                    public void ok(Void data) {
                        loadSources();
                    }

                    @Override
                    public void fail(String message) {
                        Ui.toast(ctx, message, FriendlyToast.Type.ERROR);
                    }
                })));
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

    private String readAnalysis(String json) {
        if (TextUtils.isEmpty(json)) {
            return "";
        }
        try {
            String t = json.indexOf('{') >= 0 && json.lastIndexOf('}') > json.indexOf('{')
                    ? json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1) : json;
            com.google.gson.JsonObject o = cn.yzfy.crushApp.api.GsonFactory.GSON
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

    private void loadVersions() {
        CrushApi.listVersions(crush.id, new Rest.Callback<List<Version>>() {
            @Override
            public void ok(List<Version> data) {
                versionsBox.removeAllViews();
                if (data == null || data.isEmpty()) {
                    TextView empty = new TextView(requireContext());
                    empty.setText("还没有版本。点「重建人格」生成第一版。");
                    empty.setTextSize(13);
                    empty.setTextColor(0xFFA5929C);
                    versionsBox.addView(empty);
                    return;
                }
                for (Version v : data) {
                    versionsBox.addView(versionRow(v));
                }
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private View versionRow(Version v) {
        android.content.Context ctx = requireContext();
        LinearLayout row = Ui.card(ctx, 14);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rp.topMargin = Ui.dp(ctx, 5);
        row.setLayoutParams(rp);

        TextView title = new TextView(ctx);
        title.setText("版本 " + (v.version == null ? "?" : v.version));
        title.setTextSize(14);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        row.addView(title);

        StringBuilder meta = new StringBuilder();
        if (!TextUtils.isEmpty(v.reason)) meta.append(v.reason);
        if (!TextUtils.isEmpty(v.createdAt)) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(v.createdAt);
        }
        if (meta.length() > 0) {
            TextView sub = new TextView(ctx);
            sub.setText(meta.toString());
            sub.setTextSize(11);
            sub.setTextColor(0xFFA5929C);
            row.addView(sub);
        }
        return row;
    }
}