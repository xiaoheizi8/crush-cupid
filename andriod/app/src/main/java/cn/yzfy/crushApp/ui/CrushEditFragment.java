package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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
    private LinearLayout sourcesBox;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPicker;
    private ActivityResultLauncher<String> filePicker;

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
            Ui.toast(requireContext(), "请填写名字", FriendlyToast.Type.WARN);
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

        if (crush == null) {
            String slug = slugInput.getText().toString().trim();
            if (slug.isEmpty()) {
                Ui.toast(requireContext(), "请填写 slug（唯一标识）", FriendlyToast.Type.WARN);
                return;
            }
            p.slug = slug;
            CrushApi.create(p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    Ui.toast(requireContext(), "创建成功", FriendlyToast.Type.SUCCESS);
                    requireActivity().onBackPressed();
                }

                @Override
                public void fail(String message) {
                    Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
                }
            });
        } else {
            CrushApi.update(crush.id, p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    Ui.toast(requireContext(), "已保存 ♥", FriendlyToast.Type.SUCCESS);
                    requireActivity().onBackPressed();
                }

                @Override
                public void fail(String message) {
                    Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
                }
            });
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        photoPicker = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri != null) handlePhoto(uri);
                });
        filePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) handleFile(uri);
                });
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
        addSourceRow(ctx);
    }

    private void addSourceRow(android.content.Context ctx) {
        TextView add = new TextView(ctx);
        add.setText("＋ 添加材料");
        add.setTextSize(13);
        add.setTextColor(0xFFFF5A7A);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.rounded(0xFFFFF0F3, 12));
        add.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 9), Ui.dp(ctx, 12), Ui.dp(ctx, 9));
        add.setOnClickListener(v -> addSourceDialog());
        Ui.pressScale(add);
        sourcesBox.addView(add);
    }

    private void addSourceDialog() {
        android.content.Context ctx = requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 20), Ui.dp(ctx, 18), Ui.dp(ctx, 20), Ui.dp(ctx, 18));

        TextView tip = new TextView(ctx);
        tip.setText("粘贴一段 TA 说过的话或聊天记录；也可以点下方上传图片（视觉理解）或文本文件。");
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

        TextView file = new TextView(ctx);
        file.setText("📄 文件");
        file.setTextSize(14);
        file.setTextColor(0xFF6B5E70);
        file.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        file.setOnClickListener(v -> {
            d.dismiss();
            pickFile();
        });
        btns.addView(file);

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

    private void loadSources() {
        CrushApi.listSources(crush.id, new Rest.Callback<List<Source>>() {
            @Override
            public void ok(List<Source> data) {
                if (sourcesBox == null) return;
                while (sourcesBox.getChildCount() > 1) {
                    sourcesBox.removeViewAt(1);
                }
                if (data != null) {
                    for (Source s : data) {
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
        name.setText(android.text.TextUtils.isEmpty(s.fileName) ? "文本材料" : s.fileName);
        name.setTextSize(13);
        name.setTextColor(0xFF4A4052);
        name.setPadding(Ui.dp(ctx, 8), 0, 0, 0);
        top.addView(name, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView del = new TextView(ctx);
        del.setText("删除");
        del.setTextSize(12);
        del.setTextColor(0xFFFF4D6A);
        del.setOnClickListener(v -> Ui.confirm(ctx, "删除", "删除这条材料？", "删除", () ->
                CrushApi.deleteSource(crush.id, s.id, new Rest.Callback<Void>() {
                    @Override
                    public void ok(Void data) {
                        Ui.toast(ctx, "已删除", FriendlyToast.Type.SUCCESS);
                        loadSources();
                    }

                    @Override
                    public void fail(String message) {
                        Ui.toast(ctx, message, FriendlyToast.Type.ERROR);
                    }
                })));
        top.addView(del);

        if (!android.text.TextUtils.isEmpty(s.content)) {
            TextView preview = new TextView(ctx);
            String c = s.content.length() > 60 ? s.content.substring(0, 60) + "…" : s.content;
            preview.setText(c);
            preview.setTextSize(12);
            preview.setTextColor(0xFFA5929C);
            preview.setMaxLines(2);
            preview.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(preview);
        }
        return row;
    }

    private void importText(String text) {
        CrushApi.addSource(crush.id, text, "TEXT", null, new Rest.Callback<Source>() {
            @Override
            public void ok(Source data) {
                Ui.toast(requireContext(), "材料已添加 ♥", FriendlyToast.Type.SUCCESS);
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
                Ui.toast(requireContext(), "已导入并理解 ♥", FriendlyToast.Type.SUCCESS);
                loadSources();
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
    }

    private void pickPhoto() {
        photoPicker.launch(new PickVisualMediaRequest.Builder().build());
    }

    private void pickFile() {
        filePicker.launch("*/*");
    }

    private void handlePhoto(Uri uri) {
        readUri(uri, true);
    }

    private void handleFile(Uri uri) {
        readUri(uri, false);
    }

    private void readUri(Uri uri, boolean image) {
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
            if (mime == null) mime = image ? "image/jpeg" : "application/octet-stream";
            String name = image ? System.currentTimeMillis() + ".jpg" : "";
            upload(bos.toByteArray(), name, mime);
        } catch (Exception e) {
            Ui.toast(requireContext(),
                    image ? "读取图片失败，请换一张重试" : "读取文件失败，请重试",
                    FriendlyToast.Type.ERROR);
        }
    }
}