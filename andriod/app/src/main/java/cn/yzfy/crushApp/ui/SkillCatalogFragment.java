package cn.yzfy.crushApp.ui;

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
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.SkillApi;
import cn.yzfy.crushApp.model.SkillCatalog;

/** Skill 目录 */
public class SkillCatalogFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFBF3F5);

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
        title.setText("Skill 技能包");
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        root.addView(header);

        ScrollView sv = new ScrollView(ctx);
        sv.setFillViewport(true);
        root.addView(sv, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 24));
        sv.addView(col);

        final android.app.Dialog dlg = Ui.loading(ctx, "加载技能包…");
        SkillApi.catalog(new Rest.Callback<SkillCatalog>() {
            @Override
            public void ok(final SkillCatalog data) {
                Ui.dismiss(dlg);
                if (data == null || data.skill == null) {
                    Ui.toast(requireContext(), "技能包数据为空", FriendlyToast.Type.WARN);
                    return;
                }
                Ui.post(() -> {
                    LinearLayout card = Ui.card(ctx);
                    TextView head = new TextView(ctx);
                    head.setText(data.skill.name);
                    head.setTextSize(18);
                    head.setTextColor(0xFF7256FF);
                    head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    card.addView(head);
                    if (!TextUtils.isEmpty(data.skill.description)) {
                        TextView desc = new TextView(ctx);
                        desc.setText(data.skill.description);
                        desc.setTextSize(14);
                        desc.setTextColor(0xFF4A4052);
                        desc.setLineSpacing(Ui.dp(ctx, 2), 1f);
                        desc.setPadding(0, Ui.dp(ctx, 6), 0, 0);
                        card.addView(desc);
                    }
                    if (!TextUtils.isEmpty(data.skill.version)) {
                        TextView v = new TextView(ctx);
                        v.setText("版本 " + data.skill.version);
                        v.setTextSize(12);
                        v.setTextColor(0xFFA5929C);
                        v.setPadding(0, Ui.dp(ctx, 6), 0, 0);
                        card.addView(v);
                    }
                    col.addView(card);
                    Ui.enter(card, R.anim.item_fade_slide);

                    if (data.prompts != null && !data.prompts.isEmpty()) {
                        Ui.section(ctx, "可用 Prompt");
                        for (String p : data.prompts) {
                            TextView t = Ui.chip(ctx, p);
                            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            lp.bottomMargin = Ui.dp(ctx, 6);
                            t.setLayoutParams(lp);
                            col.addView(t);
                        }
                    }

                    TextView go = new TextView(ctx);
                    go.setText("去问问军师 →");
                    go.setTextSize(15);
                    go.setTextColor(0xFFFFFFFF);
                    go.setGravity(Gravity.CENTER);
                    go.setBackground(Ui.rounded(0xFF7256FF, 14));
                    go.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 12), Ui.dp(ctx, 16), Ui.dp(ctx, 12));
                    LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    glp.topMargin = Ui.dp(ctx, 4);
                    go.setLayoutParams(glp);
                    go.setOnClickListener(v -> Nav.push(requireActivity(), AdvisorFragment.class, null));
                    Ui.pressScale(go);
                    col.addView(go);
                });
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR, true);
            }
        });
        return root;
    }
}