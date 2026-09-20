package cn.yzfy.crushApp.ui;

import android.os.Bundle;
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

import io.noties.markwon.Markwon;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.model.CrushReport;

/** 关系报告详情（Markdown 渲染） */
public class ReportDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        CrushReport r = getArguments() == null ? null
                : GsonFactory.GSON.fromJson(getArguments().getString(ReportsFragment.EXTRA_REPORT), CrushReport.class);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFDF8EF);

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
        title.setText(r == null || r.title == null || r.title.isEmpty() ? "关系报告" : r.title);
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

        TextView body = new TextView(ctx);
        body.setTextSize(15);
        body.setTextColor(0xFF3A3138);
        body.setPadding(Ui.dp(ctx, 18), Ui.dp(ctx, 16), Ui.dp(ctx, 18), Ui.dp(ctx, 32));
        body.setLineSpacing(Ui.dp(ctx, 4), 1.1f);
        sv.addView(body);
        Ui.enter(body, R.anim.fade_scale_in);

        if (r != null) {
            String md = r.markdown;
            if (md == null || md.trim().isEmpty()) {
                body.setText("（报告内容为空，可能还在生成中，返回列表刷新后重进）");
            } else {
                // 若传入的是列表项（无 markdown），拉取详情
                if (r.markdown == null) {
                    loadDetail(body, r);
                } else {
                    Markwon.create(ctx).setMarkdown(body, md);
                }
            }
        } else {
            body.setText("报告加载失败");
        }
        return root;
    }

    private void loadDetail(final TextView body, CrushReport brief) {
        cn.yzfy.crushApp.api.SkillApi.reportDetail(brief.id, new cn.yzfy.crushApp.api.Rest.Callback<CrushReport>() {
            @Override
            public void ok(CrushReport data) {
                if (data != null && data.markdown != null) {
                    Markwon.create(requireContext()).setMarkdown(body, data.markdown);
                } else {
                    body.setText("（详情为空）");
                }
            }

            @Override
            public void fail(String message) {
                body.setText("加载失败：" + message);
            }
        });
    }
}