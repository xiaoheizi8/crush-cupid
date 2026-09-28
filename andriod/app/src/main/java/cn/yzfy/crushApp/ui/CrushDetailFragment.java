package cn.yzfy.crushApp.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.Sse;
import cn.yzfy.crushApp.dto.CrushPayload;
import cn.yzfy.crushApp.model.BuildEvent;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.Source;
import cn.yzfy.crushApp.model.Version;

/** 暗恋对象资料页：资料 / 记忆 / 主动消息开关 / 构建 / 原材料 / 版本 */
public class CrushDetailFragment extends Fragment {

    private Crush crush;
    private LinearLayout col;
    private LinearLayout sourcesBox;
    private LinearLayout versionsBox;
    private LinearLayout memoryBox;
    private TextView buildText;
    private SourceImporter importer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());
        if (crush == null) {
            Ui.toast(ctx, "缺少暗恋对象参数", FriendlyToast.Type.ERROR);
        }
        importer = new SourceImporter(this, crush == null ? 0L : crush.id, this::loadSources);

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
        title.setTypeface(Typeface.DEFAULT_BOLD);
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
        buildProactiveToggle(ctx);
        buildMemory(ctx);
        buildActions(ctx);
        buildText = new TextView(ctx);
        buildText.setTextSize(12);
        buildText.setTextColor(0xFF7256FF);
        buildText.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        col.addView(buildText);

        Ui.section(ctx, "原材料");
        TextView srcTip = Ui.caption(ctx, "聊天记录 / 照片 / 朋友圈截图… 让 TA 更像 TA");
        srcTip.setPadding(Ui.dp(ctx, 16), 0, Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        col.addView(srcTip);
        sourcesBox = new LinearLayout(ctx);
        sourcesBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(sourcesBox);
        col.addView(addSourceButton(ctx));

        Ui.section(ctx, "版本历史");
        versionsBox = new LinearLayout(ctx);
        versionsBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(versionsBox);

        Ui.enter(col, R.anim.fade_scale_in);

        return root;
    }

    private View addSourceButton(android.content.Context ctx) {
        TextView add = new TextView(ctx);
        add.setText("＋ 添加原材料");
        add.setTextSize(13);
        add.setTextColor(0xFFFF5A7A);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.rounded(0xFFFFF0F3, 12));
        add.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 9), Ui.dp(ctx, 12), Ui.dp(ctx, 9));
        add.setOnClickListener(v -> importer.showDialog());
        Ui.pressScale(add);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 6);
        add.setLayoutParams(lp);
        return add;
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
        name.setTypeface(Typeface.DEFAULT_BOLD);
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

    /** 主动消息开关：crush 级 proactiveEnabled，走 PUT /api/crush/{id} */
    private void buildProactiveToggle(android.content.Context ctx) {
        LinearLayout card = Ui.card(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 10);
        card.setLayoutParams(lp);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = new TextView(ctx);
        title.setText("💌 允许 ta 主动找我");
        title.setTextSize(14);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        txt.addView(title);
        TextView sub = new TextView(ctx);
        sub.setText("开启后，ta 会在合适的时机先发消息（冷却 90 分钟，每日上限 3 次）");
        sub.setTextSize(11);
        sub.setTextColor(0xFFA5929C);
        sub.setPadding(0, Ui.dp(ctx, 2), 0, 0);
        txt.addView(sub);
        card.addView(txt);

        android.widget.Switch sw = new android.widget.Switch(ctx);
        sw.setChecked(crush != null && crush.isProactiveEnabled());
        sw.setPadding(Ui.dp(ctx, 8), 0, 0, 0);
        sw.setOnCheckedChangeListener((b, checked) -> toggleProactive(checked));
        card.addView(sw);
        col.addView(card);
    }

    private void toggleProactive(boolean enabled) {
        if (crush == null) return;
        CrushPayload p = new CrushPayload();
        p.proactiveEnabled = enabled;
        CrushApi.update(crush.id, p, new Rest.Callback<Crush>() {
            @Override
            public void ok(Crush data) {
                crush = data != null ? data : crush;
                Ui.toast(CrushDetailFragment.this, enabled ? "已开启，等 ta 先开口吧 ♥" : "已关闭主动消息",
                        FriendlyToast.Type.SUCCESS);
            }

            @Override
            public void fail(String message) {
                Ui.toast(CrushDetailFragment.this, message, FriendlyToast.Type.ERROR, true);
            }
        });
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
        memoryBox = new LinearLayout(ctx);
        memoryBox.setOrientation(LinearLayout.VERTICAL);
        memoryBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(memoryBox);
        fillMemoryCard(ctx);
    }

    /** 填充记忆卡片内容（buildMemory 与 loadCrush 刷新共用） */
    private void fillMemoryCard(android.content.Context ctx) {
        memoryBox.removeAllViews();
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
            head.setTypeface(Typeface.DEFAULT_BOLD);
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
        memoryBox.addView(card);
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

        row.addView(actionBtn(ctx, "重建人格", 0xFFFF5A7A, this::build));
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

    private void build() {
        if (crush == null) return;
        buildText.setTextColor(0xFF7256FF);
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
                        Ui.toast(CrushDetailFragment.this, "人格构建完成" + v, FriendlyToast.Type.SUCCESS);
                        Ui.post(() -> {
                            loadCrush();
                            loadSources();
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
        if (crush == null) return;
        loadCrush();
        loadSources();
        loadVersions();
    }

    /** 拉取最新 crush 并原地刷新记忆区（不再 detach/attach 重建整页） */
    private void loadCrush() {
        CrushApi.get(crush.id, new Rest.Callback<Crush>() {
            @Override
            public void ok(Crush data) {
                if (data != null) {
                    crush = data;
                    renderMemory();
                }
            }

            @Override
            public void fail(String message) {
                // 静默失败：页面已有基础数据
            }
        });
    }

    /** 原地重绘记忆卡片内容 */
    private void renderMemory() {
        if (isAdded() && memoryBox != null) {
            fillMemoryCard(requireContext());
        }
    }

    private void loadSources() {
        if (crush == null) return;
        CrushApi.listSources(crush.id, new Rest.Callback<List<Source>>() {
            @Override
            public void ok(List<Source> data) {
                if (!isAdded()) {
                    return; // sourceRow 内部需要 requireContext，detached 时直接丢弃
                }
                sourcesBox.removeAllViews();
                if (data != null) {
                    for (Source s : data) {
                        sourcesBox.addView(importer.sourceRow(s));
                    }
                }
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private void loadVersions() {
        if (crush == null) return;
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
        Ui.ripple(row);

        TextView title = new TextView(ctx);
        title.setText("版本 " + (v.version == null ? "?" : v.version));
        title.setTextSize(14);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        row.addView(title);

        StringBuilder meta = new StringBuilder();
        if (!TextUtils.isEmpty(v.reason)) meta.append(v.reason);
        if (!TextUtils.isEmpty(v.createdAt)) {
            if (meta.length() > 0) meta.append(" · ");
            meta.append(v.createdAt);
        }
        if (meta.length() > 0) {
            TextView sub = new TextView(ctx);
            sub.setText(meta.toString() + " · 点击查看快照");
            sub.setTextSize(11);
            sub.setTextColor(0xFFA5929C);
            row.addView(sub);
        }
        row.setOnClickListener(x -> showSnapshot(v));
        return row;
    }

    /** 版本快照查看：展示构建时的 Persona / Memory JSON */
    private void showSnapshot(Version v) {
        android.content.Context ctx = requireContext();
        android.app.Dialog d = new android.app.Dialog(ctx);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(0xFFFFFFFF);
        box.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 14), Ui.dp(ctx, 16), Ui.dp(ctx, 14));

        TextView head = new TextView(ctx);
        head.setText("版本 " + (v.version == null ? "?" : v.version) + " 快照");
        head.setTextSize(16);
        head.setTextColor(0xFF2A2233);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(head);

        android.widget.ScrollView sv = new android.widget.ScrollView(ctx);
        TextView body = new TextView(ctx);
        body.setText(formatSnapshot(v.snapshot));
        body.setTextSize(12);
        body.setTextColor(0xFF4A4052);
        body.setLineSpacing(Ui.dp(ctx, 2), 1f);
        body.setTextIsSelectable(true);
        sv.addView(body);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 360));
        slp.topMargin = Ui.dp(ctx, 10);
        sv.setLayoutParams(slp);
        box.addView(sv);

        TextView close = new TextView(ctx);
        close.setText("关闭");
        close.setTextSize(14);
        close.setTextColor(0xFF7256FF);
        close.setGravity(Gravity.END);
        close.setPadding(0, Ui.dp(ctx, 10), 0, 0);
        close.setOnClickListener(x -> d.dismiss());
        box.addView(close);

        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    /** 把快照 JSON 美化成可读文本 */
    private String formatSnapshot(String snapshot) {
        if (TextUtils.isEmpty(snapshot)) {
            return "（无快照内容）";
        }
        try {
            Object o = GsonFactory.GSON.fromJson(snapshot, Object.class);
            return GsonFactory.GSON.toJson(o).replace(",", ",\n").replace("{", "{\n").replace("}", "\n}");
        } catch (Exception e) {
            return snapshot;
        }
    }
}
