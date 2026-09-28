package cn.yzfy.crushApp.ui;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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

import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.dto.CrushPayload;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.Source;

/** 新建 / 编辑暗恋对象 */
public class CrushEditFragment extends Fragment {

    private Crush crush;   // null = 新建
    private EditText nameInput, slugInput, mbti, zodiac, occupation, gender, know, relation, impression, voice;
    private android.widget.Switch proactiveSwitch;
    private LinearLayout sourcesBox;
    private SourceImporter importer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());
        if (crush != null) {
            importer = new SourceImporter(this, crush.id, this::loadSources);
        }

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
        title.setText(crush == null ? "新建暗恋对象" : "编辑「" + crush.name + "」");
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sv.setLayoutParams(svlp);
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 16), Ui.dp(ctx, 24));
        sv.addView(col);
        root.addView(sv);
        Ui.enter(col, R.anim.item_fade_slide);

        nameInput = field(ctx, col, "名字 💝 *");
        nameInput.setHint("TA 的名字");
        if (crush != null) nameInput.setText(crush.name);

        slugInput = field(ctx, col, "slug 🏷 *");
        slugInput.setHint("唯一标识，如 xiaomei");
        if (crush != null) {
            slugInput.setText(crush.slug);
            slugInput.setEnabled(false);
        }

        mbti = field(ctx, col, "MBTI 🧩");
        zodiac = field(ctx, col, "星座 ✨");
        occupation = field(ctx, col, "职业 💼");
        gender = field(ctx, col, "性别 🌈");
        know = field(ctx, col, "认识多久 🕰");
        relation = field(ctx, col, "当前关系 💞");
        impression = field(ctx, col, "第一印象/备注 💌");
        voice = field(ctx, col, "音色ID 🎙");
        voice.setHint("CosyVoice voice_id，留空用默认音色");

        // 主动消息开关（与 Web 端编辑表单 / 详情页开关对齐）
        buildProactiveSection(ctx, col);

        // 材料（仅编辑已有 crush 时可用：新建还没有 id，无法关联原材料）
        if (crush != null) {
            buildSourcesSection(ctx, col);
        }

        if (crush != null) {
            mbti.setText(crush.mbti);
            zodiac.setText(crush.zodiac);
            occupation.setText(crush.occupation);
            gender.setText(crush.gender);
            know.setText(crush.knowDuration);
            relation.setText(crush.relationshipStatus);
            impression.setText(crush.impression);
            voice.setText(crush.voiceId);
            proactiveSwitch.setChecked(crush.isProactiveEnabled());
        }

        TextView save = new TextView(ctx);
        save.setText(crush == null ? "创建" : "保存修改");
        save.setTextSize(16);
        save.setTextColor(0xFFFFFFFF);
        save.setGravity(Gravity.CENTER);
        save.setBackground(Ui.rounded(0xFFFF5A7A, 14));
        save.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 12));
        save.setOnClickListener(v -> save());
        Ui.pressScale(save);
        col.addView(save, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return root;
    }

    /** 主动消息开关卡（样式与详情页开关一致，新建/编辑均可设置） */
    private void buildProactiveSection(android.content.Context ctx, LinearLayout col) {
        LinearLayout card = Ui.card(ctx);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.topMargin = Ui.dp(ctx, 16);
        card.setLayoutParams(clp);

        LinearLayout txt = new LinearLayout(ctx);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView title = new TextView(ctx);
        title.setText("💌 允许 ta 主动找我");
        title.setTextSize(14);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        txt.addView(title);
        TextView sub = new TextView(ctx);
        sub.setText("开启后，ta 会在合适的时机先发消息（冷却 90 分钟，每日上限 3 次）");
        sub.setTextSize(11);
        sub.setTextColor(0xFFA5929C);
        sub.setPadding(0, Ui.dp(ctx, 2), 0, 0);
        txt.addView(sub);
        card.addView(txt);

        proactiveSwitch = new android.widget.Switch(ctx);
        proactiveSwitch.setPadding(Ui.dp(ctx, 8), 0, 0, 0);
        card.addView(proactiveSwitch);
        col.addView(card);
    }

    private EditText field(android.content.Context ctx, LinearLayout parent, String label) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(0xFF6B5E70);
        LinearLayout.LayoutParams lpl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpl.topMargin = Ui.dp(ctx, 12);
        tv.setLayoutParams(lpl);
        parent.addView(tv);

        EditText input = new EditText(ctx);
        input.setTextSize(15);
        input.setTextColor(0xFF2A2233);
        input.setHintTextColor(0xFFC9B6BE);
        input.setElevation(Ui.dp(ctx, 1));
        input.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 10), Ui.dp(ctx, 12), Ui.dp(ctx, 10));
        input.setSingleLine(true);
        // 温馨微交互：聚焦时粉色描边 + 轻微放大，失焦柔缓恢复
        GradientDrawable normal = Ui.rounded(0xFFFFFFFF, 12);
        GradientDrawable focused = Ui.rounded(0xFFFFFFFF, 12);
        focused.setStroke(Ui.dp(ctx, 1), 0xFFFF5A7A);
        input.setBackground(normal);
        input.setOnFocusChangeListener((v, hasFocus) -> {
            v.setBackground(hasFocus ? focused : normal);
            v.animate().scaleX(hasFocus ? 1.015f : 1f)
                    .scaleY(hasFocus ? 1.015f : 1f)
                    .setDuration(140).start();
        });
        LinearLayout.LayoutParams inp_lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inp_lp.topMargin = Ui.dp(ctx, 6);
        input.setLayoutParams(inp_lp);
        parent.addView(input);
        return input;
    }

    private void save() {
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) {
            Ui.toast(CrushEditFragment.this, "请填写名字", FriendlyToast.Type.WARN);
            return;
        }
        CrushPayload p = new CrushPayload();
        p.name = name;
        p.mbti = mbti.getText().toString().trim();
        p.zodiac = zodiac.getText().toString().trim();
        p.occupation = occupation.getText().toString().trim();
        p.gender = gender.getText().toString().trim();
        p.knowDuration = know.getText().toString().trim();
        p.relationshipStatus = relation.getText().toString().trim();
        p.impression = impression.getText().toString().trim();
        p.voiceId = voice.getText().toString().trim();
        p.proactiveEnabled = proactiveSwitch.isChecked();

        if (crush == null) {
            String slug = slugInput.getText().toString().trim();
            if (slug.isEmpty()) {
                Ui.toast(CrushEditFragment.this, "请填写 slug（唯一标识）", FriendlyToast.Type.WARN);
                return;
            }
            p.slug = slug;
            CrushApi.create(p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    if (!isAdded()) {
                        return; // 已离开本页
                    }
                    Ui.toast(CrushEditFragment.this, "创建成功", FriendlyToast.Type.SUCCESS);
                    // 对齐 Web 端流程：创建后引导去完善资料 / 导入材料 / 构建人格
                    guideAfterCreate(data);
                }

                @Override
                public void fail(String message) {
                    if (isAdded()) {
                        Ui.toast(CrushEditFragment.this, message, FriendlyToast.Type.ERROR, true);
                    }
                }
            });
        } else {
            CrushApi.update(crush.id, p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    if (!isAdded()) {
                        return;
                    }
                    Ui.toast(CrushEditFragment.this, "已保存 ♥", FriendlyToast.Type.SUCCESS);
                    requireActivity().onBackPressed();
                }

                @Override
                public void fail(String message) {
                    if (isAdded()) {
                        Ui.toast(CrushEditFragment.this, message, FriendlyToast.Type.ERROR, true);
                    }
                }
            });
        }
    }

    /** 创建成功后引导：去详情页完善资料 / 导入材料 / 构建人格（对齐 Web 端「构建」流程） */
    private void guideAfterCreate(Crush created) {
        // 用户可能已离开本页（detached 后 requireContext/popBackStack 都会误伤）
        if (created == null || !isAdded()) {
            return;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("💝 " + created.name + " 已就位")
                .setMessage("接下来可以：\n· 导入 TA 的聊天记录等材料\n· 构建专属人格\n· 打开「允许 ta 主动找我」\n\n现在就去完善 TA 吗？")
                .setPositiveButton("去完善 TA", (d, w) -> {
                    // 先退回列表页再进详情，避免返回时又落到已提交的编辑页
                    requireActivity().getSupportFragmentManager().popBackStackImmediate();
                    Nav.push(requireActivity(), CrushDetailFragment.class, HomeFragment.crushArgs(created));
                })
                .setNegativeButton("稍后再说", (d, w) -> requireActivity().onBackPressed())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (crush != null) {
            loadSources();
        }
    }

    private void buildSourcesSection(android.content.Context ctx, LinearLayout col) {
        col.addView(Ui.section(ctx, "材料"));
        TextView tip = Ui.caption(ctx, "TA 说过的话、聊天记录、照片… 让 TA 更像 TA");
        tip.setPadding(Ui.dp(ctx, 16), 0, Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        col.addView(tip);
        sourcesBox = new LinearLayout(ctx);
        sourcesBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(sourcesBox);

        TextView add = new TextView(ctx);
        add.setText("＋ 添加材料");
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
        sourcesBox.addView(add);
    }

    private void loadSources() {
        CrushApi.listSources(crush.id, new Rest.Callback<List<Source>>() {
            @Override
            public void ok(List<Source> data) {
                if (sourcesBox == null) return;
                // 首行是「＋添加材料」按钮，只刷新其后的列表行
                while (sourcesBox.getChildCount() > 1) {
                    sourcesBox.removeViewAt(1);
                }
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
}