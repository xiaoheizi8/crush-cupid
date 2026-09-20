package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.VoiceApi;
import cn.yzfy.crushApp.model.VoiceConfig;

/** 音色配置：切换模型 / 挑选偏好音色 / 试听 / 用声音设计创建专属音色。 */
public class VoiceConfigFragment extends Fragment {

    private VoiceConfig config;
    private List<VoiceConfig.VoiceModel> models;
    private String currentModel;
    private String preferredVoice;

    private TextView modelText;
    private TextView voiceText;
    private LinearLayout voiceBox;
    private MediaPlayer player;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();

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
        title.setText("音色配置");
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView refresh = new TextView(ctx);
        refresh.setText("刷新");
        refresh.setTextSize(13);
        refresh.setTextColor(0xFFFFFFFF);
        refresh.setGravity(Gravity.CENTER);
        refresh.setBackground(Ui.rounded(0xFFFF5A7A, 12));
        refresh.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        refresh.setOnClickListener(v -> load());
        Ui.pressScale(refresh);
        header.addView(refresh);
        root.addView(header);

        TextView tip = new TextView(ctx);
        tip.setText("挑选心仪的声线，聊天里 TA 的语音回复就会换成它。系统音色为默认；也可用声音设计创建专属音色。");
        tip.setTextSize(12);
        tip.setTextColor(0xFFA5929C);
        tip.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        root.addView(tip);

        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(ctx);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 4), Ui.dp(ctx, 12), Ui.dp(ctx, 16));

        // 模型卡片
        LinearLayout modelCard = Ui.card(ctx);
        modelText = new TextView(ctx);
        modelText.setTextSize(15);
        modelText.setTextColor(0xFF2A2233);
        modelText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        modelCard.addView(modelText);
        TextView switchModel = actionBtn(ctx, "切换模型");
        switchModel.setOnClickListener(v -> pickModel());
        modelCard.addView(switchModel);
        content.addView(modelCard);

        // 我的音色卡片
        LinearLayout voiceCard = Ui.card(ctx);
        voiceText = new TextView(ctx);
        voiceText.setTextSize(15);
        voiceText.setTextColor(0xFFFF5A7A);
        voiceText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        voiceCard.addView(voiceText);
        content.addView(voiceCard);

        // 音色选择区
        voiceBox = new LinearLayout(ctx);
        voiceBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(voiceBox);

        scroll.addView(content);
        Ui.enter(content, R.anim.fade_scale_in);
        root.addView(scroll);
        return root;
    }

    private TextView actionBtn(android.content.Context ctx, String text) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(0xFF7256FF);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.rounded(0xFFEFEBFF, 999));
        t.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 8);
        t.setLayoutParams(lp);
        Ui.pressScale(t);
        return t;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        final Dialog dlg = Ui.loading(requireContext(), "加载音色…");
        VoiceApi.getConfig(new Rest.Callback<VoiceConfig>() {
            @Override
            public void ok(VoiceConfig data) {
                Ui.dismiss(dlg);
                config = data;
                preferredVoice = data != null ? data.currentVoice : null;
                if (data != null) {
                    models = data.availableModels;
                    if (models != null && !models.isEmpty()) {
                        currentModel = models.get(0).name;
                        for (VoiceConfig.VoiceModel m : models) {
                            if (m.name != null && m.name.equals(data.currentModel)) {
                                currentModel = m.name;
                                break;
                            }
                        }
                    }
                }
                render();
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private void render() {
        if (config == null) {
            return;
        }
        modelText.setText("当前模型：" + (currentModel == null ? "未知" : currentModel));
        voiceText.setText(preferredVoice == null || preferredVoice.isEmpty()
                ? "我的音色：未设置（用系统默认）" : "我的音色：" + preferredVoice);

        voiceBox.removeAllViews();
        TextView h = new TextView(requireContext());
        h.setText("可选音色");
        h.setTextSize(15);
        h.setTextColor(0xFF2A2233);
        h.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        h.setPadding(0, Ui.dp(requireContext(), 10), 0, Ui.dp(requireContext(), 4));
        voiceBox.addView(h);

        List<VoiceConfig.VoiceOption> voices = collectVoices();
        if (voices == null || voices.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("未获取到该模型的音色。\n可以试试点「声音设计」创建专属音色。");
            empty.setTextSize(13);
            empty.setTextColor(0xFFB8A5AC);
            empty.setPadding(Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 8),
                    Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 8));
            voiceBox.addView(empty);
        } else {
            for (VoiceConfig.VoiceOption v : voices) {
                voiceBox.addView(voiceRow(v));
            }
        }

        voiceBox.addView(Ui.space(requireContext(), 6));
        voiceBox.addView(buildDesignCard());
    }

    private List<VoiceConfig.VoiceOption> collectVoices() {
        List<VoiceConfig.VoiceOption> out = new ArrayList<>();
        if (models == null) {
            if (config != null && config.availableVoices != null) {
                return config.availableVoices;
            }
            return out;
        }
        for (VoiceConfig.VoiceModel m : models) {
            if (m.name != null && m.name.equals(currentModel)) {
                if (m.voices != null) {
                    out.addAll(m.voices);
                }
            }
        }
        if (out.isEmpty()) {
            // 兜底：展示全部模型音色
            for (VoiceConfig.VoiceModel m : models) {
                if (m.voices != null) {
                    out.addAll(m.voices);
                }
            }
        }
        return out;
    }

    private View voiceRow(final VoiceConfig.VoiceOption v) {
        android.content.Context ctx = requireContext();
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(Ui.rounded(0xFFFFFFFF, 14));
        row.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 10), Ui.dp(ctx, 12), Ui.dp(ctx, 10));
        Ui.ripple(row);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(ctx, 6);
        row.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(clp);
        TextView name = new TextView(ctx);
        name.setText((v.name == null || v.name.isEmpty() ? v.voiceId : v.name)
                + (v.voiceId != null && v.voiceId.equals(preferredVoice) ? "  ✓ 已选" : ""));
        name.setTextSize(14);
        name.setTextColor(0xFF2A2233);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        col.addView(name);
        TextView sub = new TextView(ctx);
        StringBuilder s = new StringBuilder();
        if (v.gender != null && !v.gender.isEmpty()) s.append(v.gender);
        if (v.model != null && !v.model.isEmpty()) s.append(s.length() == 0 ? "" : " · ").append(v.model);
        sub.setText(s.length() == 0 ? "系统音色" : s.toString());
        sub.setTextSize(11);
        sub.setTextColor(0xFFA5929C);
        col.addView(sub);
        row.addView(col);

        TextView play = miniBtn(ctx, "试听", 0xFF2FBF71);
        play.setOnClickListener(v14 -> preview(v));
        row.addView(play);

        TextView use = miniBtn(ctx, v.voiceId != null && v.voiceId.equals(preferredVoice) ? "已选用" : "选用", 0xFFFF5A7A);
        use.setOnClickListener(v13 -> select(v));
        row.addView(use);
        return row;
    }

    private TextView miniBtn(android.content.Context ctx, String text, int color) {
        TextView t = new TextView(ctx);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(0xFFFFFFFF);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.rounded(color, 999));
        t.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 5), Ui.dp(ctx, 10), Ui.dp(ctx, 5));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(ctx, 6);
        t.setLayoutParams(lp);
        return t;
    }

    private void pickModel() {
        if (models == null || models.isEmpty()) {
            Ui.toast(requireContext(), "暂无可用模型", FriendlyToast.Type.WARN);
            return;
        }
        String[] labels = new String[models.size()];
        for (int i = 0; i < models.size(); i++) {
            VoiceConfig.VoiceModel m = models.get(i);
            labels[i] = m.displayName != null && !m.displayName.isEmpty() ? m.displayName : m.name;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择模型")
                .setItems(labels, (d, w) -> {
                    currentModel = models.get(w).name;
                    render();
                })
                .show();
    }

    private void select(final VoiceConfig.VoiceOption v) {
        if (v.voiceId == null || v.voiceId.isEmpty()) {
            Ui.toast(requireContext(), "该音色缺少 voiceId", FriendlyToast.Type.WARN);
            return;
        }
        final Dialog dlg = Ui.loading(requireContext(), "保存中…");
        VoiceApi.saveConfig(v.voiceId, new Rest.Callback<VoiceConfig>() {
            @Override
            public void ok(VoiceConfig data) {
                Ui.dismiss(dlg);
                VoiceApi.cacheVoice(v.voiceId);
                preferredVoice = v.voiceId;
                render();
                Ui.toast(requireContext(), "已设为我的音色", FriendlyToast.Type.SUCCESS);
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private void preview(final VoiceConfig.VoiceOption v) {
        Ui.toast(requireContext(), "正在试听…");
        VoiceApi.synthesize("嗨，这是我现在的声线，你觉得怎么样？", v.voiceId, new Rest.Callback<String>() {
            @Override
            public void ok(String base64) {
                playMp3(base64);
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), "试听失败：" + message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private View buildDesignCard() {
        android.content.Context ctx = requireContext();
        LinearLayout card = Ui.card(ctx);
        TextView h = new TextView(ctx);
        h.setText("✨ 声音设计 · 创建专属音色");
        h.setTextSize(15);
        h.setTextColor(0xFF2A2233);
        h.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        card.addView(h);

        TextView h1 = new TextView(ctx);
        h1.setText("描述想要的声线（≤500 字）");
        h1.setTextSize(12);
        h1.setTextColor(0xFF6B5E70);
        LinearLayout.LayoutParams h1lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        h1lp.topMargin = Ui.dp(ctx, 8);
        h1.setLayoutParams(h1lp);
        card.addView(h1);
        final EditText prompt = input(ctx, "如：温柔的年轻女性，语速轻快，带笑意");
        card.addView(prompt);

        TextView h2 = new TextView(ctx);
        h2.setText("试听文本（选填）");
        h2.setTextSize(12);
        h2.setTextColor(0xFF6B5E70);
        LinearLayout.LayoutParams h2lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        h2lp.topMargin = Ui.dp(ctx, 8);
        h2.setLayoutParams(h2lp);
        card.addView(h2);
        final EditText previewText = input(ctx, "如：嘿，今天有没有想我？");
        card.addView(previewText);

        TextView create = actionBtn(ctx, "创建专属音色");
        create.setTextColor(0xFFFFFFFF);
        create.setBackground(Ui.rounded(0xFFFF5A7A, 12));
        create.setOnClickListener(v -> {
            String p = prompt.getText().toString().trim();
            if (p.isEmpty()) {
                Ui.toast(requireContext(), "请先描述想要的声线", FriendlyToast.Type.WARN);
                return;
            }
            final Dialog dlg = Ui.loading(requireContext(), "生成音色中，约需 10~30 秒…");
            VoiceApi.design(p, previewText.getText().toString().trim(), new Rest.Callback<String>() {
                @Override
                public void ok(String voiceId) {
                    saveDesignVoice(voiceId, dlg);
                }

                @Override
                public void fail(String message) {
                    Ui.dismiss(dlg);
                    Ui.toast(requireContext(), "声音设计失败：" + message, FriendlyToast.Type.ERROR, true);
                }
            });
        });
        card.addView(create);
        return card;
    }

    private void saveDesignVoice(final String voiceId, final Dialog dlg) {
        VoiceApi.saveConfig(voiceId, new Rest.Callback<VoiceConfig>() {
            @Override
            public void ok(VoiceConfig data) {
                Ui.dismiss(dlg);
                VoiceApi.cacheVoice(voiceId);
                preferredVoice = voiceId;
                render();
                Ui.toast(requireContext(), "专属音色已创建并启用 ✅", FriendlyToast.Type.SUCCESS);
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), "音色已生成但保存失败：" + message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private EditText input(android.content.Context ctx, String hint) {
        EditText e = new EditText(ctx);
        e.setHint(hint);
        e.setTextSize(14);
        e.setTextColor(0xFF2A2233);
        e.setHintTextColor(0xFFC9B6BE);
        e.setBackground(Ui.rounded(0xFFFBF6F7, 10));
        e.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        e.setMaxLines(3);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 4);
        e.setLayoutParams(lp);
        return e;
    }

    private void playMp3(final String base64) {
        Ui.post(() -> {
            try {
                releasePlayer();
                File f = new File(requireContext().getCacheDir(), "voice_" + System.currentTimeMillis() + ".mp3");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(android.util.Base64.decode(base64, android.util.Base64.DEFAULT));
                fos.close();
                player = new MediaPlayer();
                player.setDataSource(f.getAbsolutePath());
                player.setOnCompletionListener(mp -> releasePlayer());
                player.setOnErrorListener((mp, w, e) -> {
                    releasePlayer();
                    return true;
                });
                player.prepare();
                player.start();
            } catch (Exception e) {
                Ui.toast(requireContext(), "播放失败，请稍后再试", FriendlyToast.Type.ERROR);
            }
        });
    }

    private void releasePlayer() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
        super.onDestroyView();
    }
}