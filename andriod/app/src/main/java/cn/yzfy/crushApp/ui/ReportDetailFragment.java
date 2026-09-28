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

    private CrushReport r;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        r = getArguments() == null ? null
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

        // 导出 Word（与 Web 端「导出 .docx」对齐）：下载到应用文档目录后调起系统预览
        if (r != null && r.id != null) {
            TextView export = new TextView(ctx);
            export.setText("导出 Word");
            export.setTextSize(13);
            export.setTextColor(0xFFFFFFFF);
            export.setGravity(Gravity.CENTER);
            export.setBackground(Ui.rounded(0xFF7256FF, 12));
            export.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
            export.setOnClickListener(v -> exportDocx());
            Ui.pressScale(export);
            header.addView(export);
        }
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
                // 列表项不带正文（服务端 listReports 省略 markdown），先占位再拉详情
                body.setText("（报告加载中…）");
                loadDetail(body, r);
            } else {
                Markwon.create(ctx).setMarkdown(body, md);
            }
        } else {
            body.setText("报告加载失败");
        }
        return root;
    }

    /** 导出当前报告为 .docx 并尝试打开 */
    private void exportDocx() {
        android.content.Context ctx = requireContext();
        CrushReport cur = r;
        if (cur == null || cur.id == null) {
            return;
        }
        android.app.Dialog dlg = Ui.loading(ctx, "正在导出…");
        cn.yzfy.crushApp.api.SkillApi.downloadReport(ctx, cur.id, cur.title,
                new cn.yzfy.crushApp.api.SkillApi.FileCallback() {
                    @Override
                    public void ok(java.io.File file) {
                        Ui.dismiss(dlg);
                        Ui.toast(ctx, "已导出：" + file.getName(), FriendlyToast.Type.SUCCESS);
                        openDocx(ctx, file);
                    }

                    @Override
                    public void fail(String message) {
                        Ui.dismiss(dlg);
                        Ui.toast(ctx, message, FriendlyToast.Type.ERROR, true);
                    }
                });
    }

    /** FileProvider 调起第三方应用打开 docx；无应用时仅提示已保存 */
    private void openDocx(android.content.Context ctx, java.io.File file) {
        try {
            android.net.Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    ctx, ctx.getPackageName() + ".fileprovider", file);
            android.content.Intent it = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            it.setDataAndType(uri, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            it.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(it);
        } catch (Exception e) {
            Ui.toast(ctx, "未找到可打开 Word 的应用，文件已保存在应用文档目录", FriendlyToast.Type.INFO);
        }
    }

    private void loadDetail(final TextView body, CrushReport brief) {
        if (brief.id == null) {
            body.setText("（报告内容为空）");
            return;
        }
        cn.yzfy.crushApp.api.SkillApi.reportDetail(brief.id, new cn.yzfy.crushApp.api.Rest.Callback<CrushReport>() {
            @Override
            public void ok(CrushReport data) {
                if (!isAdded()) {
                    return; // 已离开页面，丢弃回调
                }
                if (data != null && data.markdown != null) {
                    Markwon.create(requireContext()).setMarkdown(body, data.markdown);
                } else {
                    body.setText("（详情为空）");
                }
            }

            @Override
            public void fail(String message) {
                if (!isAdded()) {
                    return;
                }
                body.setText("加载失败：" + message);
            }
        });
    }
}