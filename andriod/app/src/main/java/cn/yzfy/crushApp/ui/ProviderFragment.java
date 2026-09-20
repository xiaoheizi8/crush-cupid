package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.ProviderApi;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.model.AiProvider;

/** 自定义 LLM 供应商管理（能力：视觉看图 / 音频听语音） */
public class ProviderFragment extends Fragment {

    private final List<AiProvider> items = new ArrayList<>();
    private ProviderAdapter adapter;
    private TextView empty;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh;

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
        title.setText("模型供应商");
        title.setTextSize(17);
        title.setTextColor(0xFF2A2233);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView add = new TextView(ctx);
        add.setText("＋ 新增");
        add.setTextSize(13);
        add.setTextColor(0xFFFFFFFF);
        add.setGravity(Gravity.CENTER);
        add.setBackground(Ui.rounded(0xFFFF5A7A, 12));
        add.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 6));
        add.setOnClickListener(v -> edit(null));
        Ui.pressScale(add);
        header.addView(add);
        root.addView(header);

        TextView tip = new TextView(ctx);
        tip.setText("配置你自己的 deepseek / qwen-vl / openai 等大模型 API，运行时立即生效。文本是所有模型的基本能力。");
        tip.setTextSize(12);
        tip.setTextColor(0xFFA5929C);
        tip.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 4));
        root.addView(tip);

        RecyclerView list = new RecyclerView(ctx);
        list.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        list.setLayoutManager(new LinearLayoutManager(ctx));
        list.setClipToPadding(false);
        list.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 4), Ui.dp(ctx, 12), Ui.dp(ctx, 12));
        adapter = new ProviderAdapter();
        list.setAdapter(adapter);
        Ui.listEnter(list, R.anim.layout_list);
        refresh = Ui.pullRefresh(ctx, list, this::load);
        root.addView(refresh, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        empty = new TextView(ctx);
        empty.setText("还没有自定义供应商\n点右上角「＋ 新增」接入第一个模型");
        empty.setTextSize(14);
        empty.setTextColor(0xFFB8A5AC);
        empty.setGravity(Gravity.CENTER);
        empty.setVisibility(View.GONE);
        root.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        ProviderApi.list(new Rest.Callback<List<AiProvider>>() {
            @Override
            public void ok(List<AiProvider> data) {
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

    private void edit(final AiProvider p) {
        android.content.Context ctx = requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 22), Ui.dp(ctx, 18), Ui.dp(ctx, 22), Ui.dp(ctx, 18));

        TextView head = new TextView(ctx);
        head.setText(p == null ? "新增供应商" : "编辑供应商");
        head.setTextSize(17);
        head.setTextColor(0xFF2A2233);
        head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(head);

        final EditText name = field(box, ctx, "名称（如 deepseek）", p == null ? null : p.name);
        final EditText key = field(box, ctx, "供应商代号（providerKey）", p == null ? null : p.providerKey);
        if (p != null) {
            key.setEnabled(false);
            key.setTextColor(0xFF6B5E70);
        }
        final EditText baseUrl = field(box, ctx, "Base URL", p == null ? null : p.baseUrl);
        final EditText apiKey = field(box, ctx, "apiKey", p == null ? null : p.apiKey);
        final EditText model = field(box, ctx, "model 名称", p == null ? null : p.model);
        final EditText temp = field(box, ctx, "temperature（选填）", p == null || p.temperature == null ? null : String.valueOf(p.temperature));
        final EditText topP = field(box, ctx, "topP（选填）", p == null || p.topP == null ? null : String.valueOf(p.topP));
        final EditText maxTokens = field(box, ctx, "maxTokens（选填）", p == null || p.maxTokens == null ? null : String.valueOf(p.maxTokens));

        final CheckBox vision = new CheckBox(ctx);
        vision.setText("视觉（看图识图）");
        vision.setChecked(p != null && p.has("vision"));
        box.addView(vision);

        final CheckBox audio = new CheckBox(ctx);
        audio.setText("音频（听语音/语音输入）");
        audio.setChecked(p != null && p.has("audio"));
        box.addView(audio);

        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(ctx, 6);
        btns.setLayoutParams(blp);

        TextView cancel = new TextView(ctx);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setTextColor(0xFFA5929C);
        cancel.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 8), Ui.dp(ctx, 14), Ui.dp(ctx, 8));
        cancel.setOnClickListener(v -> d.dismiss());
        btns.addView(cancel);

        TextView ok = new TextView(ctx);
        ok.setText(p == null ? "创建" : "保存");
        ok.setTextSize(14);
        ok.setTextColor(0xFFFFFFFF);
        ok.setBackground(Ui.rounded(0xFFFF5A7A, 12));
        ok.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 8));
        ok.setOnClickListener(v -> {
            AiProvider n = new AiProvider();
            if (p != null) n.id = p.id;
            n.name = name.getText().toString().trim();
            n.providerKey = key.getText().toString().trim();
            n.baseUrl = baseUrl.getText().toString().trim();
            n.apiKey = apiKey.getText().toString().trim();
            n.model = model.getText().toString().trim();
            try {
                if (!temp.getText().toString().trim().isEmpty()) n.temperature = Double.parseDouble(temp.getText().toString().trim());
                if (!topP.getText().toString().trim().isEmpty()) n.topP = Double.parseDouble(topP.getText().toString().trim());
                if (!maxTokens.getText().toString().trim().isEmpty()) n.maxTokens = Integer.parseInt(maxTokens.getText().toString().trim());
            } catch (NumberFormatException ignored) {
            }
            List<String> caps = new ArrayList<>();
            if (vision.isChecked()) caps.add("vision");
            if (audio.isChecked()) caps.add("audio");
            n.capabilities = caps;
            if (n.name.isEmpty() || n.providerKey.isEmpty() || n.baseUrl.isEmpty() || n.model.isEmpty()) {
                Ui.toast(ctx, "名称/代号/BaseURL/model 必填");
                return;
            }
            d.dismiss();
            android.app.Dialog dlg = Ui.loading(ctx, "保存中…");
            if (p == null) {
                ProviderApi.create(n, saveCb(dlg));
            } else {
                ProviderApi.update(p.id, n, saveCb(dlg));
            }
        });
        btns.addView(ok);
        box.addView(btns);

        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    private Rest.Callback<AiProvider> saveCb(final Dialog dlg) {
        return new Rest.Callback<AiProvider>() {
            @Override
            public void ok(AiProvider data) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), "已保存，即时生效");
                load();
            }

            @Override
            public void fail(String message) {
                Ui.dismiss(dlg);
                Ui.toast(requireContext(), message, true);
            }
        };
    }

    private EditText field(LinearLayout parent, android.content.Context ctx, String label, String value) {
        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(12);
        tv.setTextColor(0xFF6B5E70);
        LinearLayout.LayoutParams lpl = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpl.topMargin = Ui.dp(ctx, 8);
        tv.setLayoutParams(lpl);
        parent.addView(tv);

        EditText input = new EditText(ctx);
        input.setTextSize(14);
        input.setTextColor(0xFF2A2233);
        if (value != null) input.setText(value);
        input.setBackground(Ui.rounded(0xFFFBF6F7, 10));
        input.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = Ui.dp(ctx, 4);
        input.setLayoutParams(ilp);
        parent.addView(input);
        return input;
    }

    private class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            final LinearLayout row;

            Holder(View v) {
                super(v);
                row = (LinearLayout) v;
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

            TextView nameBox = new TextView(ctx);
            nameBox.setTag("name");
            nameBox.setTextSize(15);
            nameBox.setTextColor(0xFF2A2233);
            nameBox.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            row.addView(nameBox);
            TextView sub = new TextView(ctx);
            sub.setTag("sub");
            sub.setTextSize(12);
            sub.setTextColor(0xFF6B5E70);
            row.addView(sub);
            LinearLayout caps = new LinearLayout(ctx);
            caps.setTag("caps");
            caps.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams cplp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cplp.topMargin = Ui.dp(ctx, 6);
            caps.setLayoutParams(cplp);
            row.addView(caps);
            return new Holder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final AiProvider p = items.get(position);
            TextView name = (TextView) h.row.getChildAt(0);
            TextView sub = (TextView) h.row.getChildAt(1);
            LinearLayout capsBox = (LinearLayout) h.row.getChildAt(2);

            name.setText(p.name == null ? "" : p.name);
            sub.setText((p.providerKey == null ? "" : p.providerKey) + " · " + (p.model == null ? "" : p.model));

            capsBox.removeAllViews();
            if (p.has("vision")) addCap(capsBox, "👀 看图");
            if (p.has("audio")) addCap(capsBox, "🎙 听语音");
            addCap(capsBox, "🅃 文本");
            if (capsBox.getChildCount() == 0) {
                addCap(capsBox, "文本");
            }

            h.row.setOnClickListener(v -> edit(p));
            h.row.setOnLongClickListener(v -> {
                Ui.confirm(requireContext(), "删除供应商", "确定删除「" + p.name + "」？", "删除", () ->
                        ProviderApi.delete(p.id, new Rest.Callback<Void>() {
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

        private void addCap(LinearLayout box, String label) {
            TextView t = new TextView(box.getContext());
            t.setText(label);
            t.setTextSize(11);
            t.setTextColor(0xFFFF5A7A);
            t.setPadding(Ui.dp(box.getContext(), 8), Ui.dp(box.getContext(), 3),
                    Ui.dp(box.getContext(), 8), Ui.dp(box.getContext(), 3));
            t.setBackground(Ui.rounded(0xFFFFF0F3, 999));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = Ui.dp(box.getContext(), 6);
            t.setLayoutParams(lp);
            box.addView(t);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}