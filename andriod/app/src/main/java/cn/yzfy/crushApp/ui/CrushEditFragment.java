package cn.yzfy.crushApp.ui;

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

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.dto.CrushPayload;
import cn.yzfy.crushApp.model.Crush;

/** 新建 / 编辑暗恋对象 */
public class CrushEditFragment extends Fragment {

    private Crush crush;   // null = 新建
    private EditText nameInput, slugInput, mbti, zodiac, occupation, gender, know, relation, impression;

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

        nameInput = field(ctx, col, "名字 *");
        nameInput.setHint("TA 的名字");
        if (crush != null) nameInput.setText(crush.name);

        slugInput = field(ctx, col, "slug *");
        slugInput.setHint("唯一标识，如 xiaomei");
        if (crush != null) {
            slugInput.setText(crush.slug);
            slugInput.setEnabled(false);
        }

        mbti = field(ctx, col, "MBTI");
        zodiac = field(ctx, col, "星座");
        occupation = field(ctx, col, "职业");
        gender = field(ctx, col, "性别");
        know = field(ctx, col, "认识多久");
        relation = field(ctx, col, "当前关系");
        impression = field(ctx, col, "第一印象/备注");

        if (crush != null) {
            mbti.setText(crush.mbti);
            zodiac.setText(crush.zodiac);
            occupation.setText(crush.occupation);
            gender.setText(crush.gender);
            know.setText(crush.knowDuration);
            relation.setText(crush.relationshipStatus);
            impression.setText(crush.impression);
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
        input.setBackground(Ui.rounded(0xFFFFFFFF, 12));
        input.setElevation(Ui.dp(ctx, 1));
        input.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 10), Ui.dp(ctx, 12), Ui.dp(ctx, 10));
        input.setSingleLine(true);
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
            Ui.toast(requireContext(), "请填写名字");
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

        if (crush == null) {
            String slug = slugInput.getText().toString().trim();
            if (slug.isEmpty()) {
                Ui.toast(requireContext(), "请填写 slug（唯一标识）");
                return;
            }
            p.slug = slug;
            CrushApi.create(p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    Ui.toast(requireContext(), "创建成功");
                    requireActivity().onBackPressed();
                }

                @Override
                public void fail(String message) {
                    Ui.toast(requireContext(), message, true);
                }
            });
        } else {
            CrushApi.update(crush.id, p, new Rest.Callback<Crush>() {
                @Override
                public void ok(Crush data) {
                    Ui.toast(requireContext(), "已保存");
                    requireActivity().onBackPressed();
                }

                @Override
                public void fail(String message) {
                    Ui.toast(requireContext(), message, true);
                }
            });
        }
    }
}