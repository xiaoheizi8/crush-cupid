package cn.yzfy.crushApp.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.SkillApi;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.CrushReport;

/** 关系报告列表 + 生成 */
public class ReportsFragment extends Fragment {

    public static final String EXTRA_REPORT = "report";

    private final List<CrushReport> items = new ArrayList<>();
    private Crush crush;
    private RecyclerView list;
    private ReportAdapter adapter;
    private TextView empty;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6FAF4);

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
        title.setText("关系报告");
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView gen = new TextView(ctx);
        gen.setText("生成报告");
        gen.setTextSize(13);
        gen.setTextColor(0xFFFFFFFF);
        gen.setGravity(Gravity.CENTER);
        gen.setBackground(Ui.rounded(0xFF2FBF71, 12));
        gen.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        gen.setOnClickListener(v -> generate());
        Ui.pressScale(gen);
        header.addView(gen);
        root.addView(header);

        list = new RecyclerView(ctx);
        list.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list.setLayoutManager(new LinearLayoutManager(ctx));
        list.setClipToPadding(false);
        list.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 8), Ui.dp(ctx, 12), Ui.dp(ctx, 8));
        adapter = new ReportAdapter();
        list.setAdapter(adapter);
        Ui.listEnter(list, R.anim.layout_list);
        refresh = Ui.pullRefresh(ctx, list, this::load);
        root.addView(refresh, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        empty = new TextView(ctx);
        empty.setText("还没有关系报告。\n点右上角「生成报告」用 AI 复盘你们的关系。");
        empty.setTextSize(14);
        empty.setTextColor(0xFFA8BDAF);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        root.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    private void generate() {
        if (crush == null) {
            return;
        }
        android.app.Dialog dlg = Ui.loading(requireContext(), "军师正在撰写关系报告…");
        SkillApi.generateReport(crush.slug, new Rest.Callback<CrushReport>() {
            @Override
            public void ok(CrushReport data) {
                Ui.dismiss(dlg);
                if (data != null) {
                    openReport(data);
                } else {
                    Ui.toast(requireContext(), "报告为空");
                }
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), message, true);
            }
        });
    }

    private void openReport(CrushReport r) {
        Bundle b = new Bundle();
        b.putString(EXTRA_REPORT, GsonFactory.GSON.toJson(r));
        Nav.push(requireActivity(), ReportDetailFragment.class, b);
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        if (crush == null) {
            if (refresh != null) refresh.setRefreshing(false);
            return;
        }
        SkillApi.reports(crush.slug, new Rest.Callback<List<CrushReport>>() {
            @Override
            public void ok(List<CrushReport> data) {
                items.clear();
                if (data != null) items.addAll(data);
                adapter.notifyDataSetChanged();
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                if (refresh != null) refresh.setRefreshing(false);
            }

            @Override
            public void fail(String message) {
                if (refresh != null) refresh.setRefreshing(false);
                Ui.toast(requireContext(), message, true);
            }
        });
    }

    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            Holder(View v) {
                super(v);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.content.Context ctx = parent.getContext();
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setBackground(Ui.rounded(0xFFFFFFFF, 16));
            row.setElevation(Ui.dp(ctx, 1));
            row.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 12), Ui.dp(ctx, 14), Ui.dp(ctx, 12));
            Ui.ripple(row);
            RecyclerView.LayoutParams rp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.bottomMargin = Ui.dp(ctx, 8);
            row.setLayoutParams(rp);

            TextView title = new TextView(ctx);
            title.setTag("title");
            title.setTextSize(15);
            title.setTextColor(0xFF2A2233);
            title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            row.addView(title);
            TextView sub = new TextView(ctx);
            sub.setTag("sub");
            sub.setTextSize(12);
            sub.setTextColor(0xFFA5929C);
            row.addView(sub);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final CrushReport r = items.get(position);
            LinearLayout row = (LinearLayout) h.itemView;
            TextView title = (TextView) row.getChildAt(0);
            TextView sub = (TextView) row.getChildAt(1);

            StringBuilder s = new StringBuilder();
            if (!TextUtils.isEmpty(r.source)) s.append(r.source);
            if (!TextUtils.isEmpty(r.reportDate)) s.append(s.length() > 0 ? " · " : "").append(r.reportDate);
            if (!TextUtils.isEmpty(r.createdAt)) s.append(s.length() > 0 ? " · " : "").append("报告于 ").append(r.createdAt);
            if (s.length() > 0) sub.setText(s.toString());
            title.setText(TextUtils.isEmpty(r.title) ? "关系报告" : r.title);

            row.setOnClickListener(v -> openReport(r));
            row.setOnLongClickListener(v -> {
                Ui.confirm(requireContext(), "删除报告", "确定删除这份报告吗？", "删除", () ->
                        SkillApi.deleteReport(r.id, new Rest.Callback<Void>() {
                            @Override
                            public void ok(Void data) {
                                load();
                            }

                            @Override
                            public void fail(String message) {
                                Ui.toast(requireContext(), message);
                            }
                        }));
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}