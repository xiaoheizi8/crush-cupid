package cn.yzfy.crushApp.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
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
import cn.yzfy.crushApp.api.ChatApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.SkillApi;
import cn.yzfy.crushApp.api.Sse;
import cn.yzfy.crushApp.model.AdvisorCommand;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.MultiChunk;

/** 军师页：子命令卡片 + 自由对话（独立记忆） */
public class AdvisorFragment extends Fragment {

    private final List<String> log = new ArrayList<>();
    private Crush crush;
    private RecyclerView list;
    private AdvisorMsgAdapter adapter;
    private LinearLayoutManager layoutManager;
    private EditText input;
    private boolean streaming;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFF6F3FF);

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
        LinearLayout nc = new LinearLayout(ctx);
        nc.setOrientation(LinearLayout.VERTICAL);
        nc.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(ctx);
        t.setText("军师 🤵");
        t.setTextSize(17);
        t.setTextColor(0xFF7256FF);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        nc.addView(t);
        TextView st = new TextView(ctx);
        st.setText("为你出谋划策 · 独立记忆");
        st.setTextSize(11);
        st.setTextColor(0xFFA5929C);
        nc.addView(st);
        header.addView(nc);
        root.addView(header);

        // 命令卡片（横向滚动）
        LinearLayout cmdRow = new LinearLayout(ctx);
        cmdRow.setOrientation(LinearLayout.HORIZONTAL);
        cmdRow.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10));
        HorizontalScrollView cmdScroll = new HorizontalScrollView(ctx);
        cmdScroll.setFillViewport(false);
        cmdScroll.setHorizontalScrollBarEnabled(false);
        cmdScroll.addView(cmdRow);
        root.addView(cmdScroll);

        list = new RecyclerView(ctx);
        list.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        layoutManager = new LinearLayoutManager(ctx);
        layoutManager.setStackFromEnd(true);
        list.setLayoutManager(layoutManager);
        list.setClipToPadding(false);
        list.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 4), Ui.dp(ctx, 12), Ui.dp(ctx, 4));
        adapter = new AdvisorMsgAdapter();
        list.setAdapter(adapter);
        root.addView(list);

        LinearLayout composer = new LinearLayout(ctx);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setBackgroundColor(0xFFFFFFFF);
        composer.setElevation(Ui.dp(ctx, 3));
        composer.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        input = new EditText(ctx);
        input.setHint("问军师一个问题…");
        input.setTextSize(15);
        input.setBackground(Ui.rounded(0xFFF6F3FF, 14));
        input.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 8), Ui.dp(ctx, 12), Ui.dp(ctx, 8));
        input.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        composer.addView(input);
        TextView send = new TextView(ctx);
        send.setText("发送");
        send.setTextSize(15);
        send.setTextColor(0xFFFFFFFF);
        send.setGravity(Gravity.CENTER);
        send.setBackground(Ui.rounded(0xFF7256FF, 14));
        send.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 9), Ui.dp(ctx, 16), Ui.dp(ctx, 9));
        Ui.pressScale(send);
        send.setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) ask(text);
        });
        composer.addView(send);
        root.addView(composer);

        loadCommands(cmdRow);
        return root;
    }

    private void loadCommands(final LinearLayout cmdRow) {
        SkillApi.advisorCommands(new Rest.Callback<List<AdvisorCommand>>() {
            @Override
            public void ok(List<AdvisorCommand> data) {
                cmdRow.removeAllViews();
                if (data == null) {
                    return;
                }
                for (final AdvisorCommand c : data) {
                    TextView chip = new TextView(requireContext());
                    chip.setText(c.title);
                    chip.setTextSize(14);
                    chip.setTextColor(0xFF7256FF);
                    chip.setGravity(Gravity.CENTER);
                    chip.setBackground(Ui.rounded(0xFFEFEBFF, 999));
                    chip.setPadding(Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 8),
                            Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 8));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.rightMargin = Ui.dp(requireContext(), 8);
                    chip.setLayoutParams(lp);
                    chip.setOnClickListener(v -> invoke(c));
                    Ui.pressScale(chip);
                    cmdRow.addView(chip);
                }
            }

            @Override
            public void fail(String message) {
            }
        });
    }

    private void invoke(AdvisorCommand c) {
        if (streaming) {
            Ui.toast(requireContext(), "军师正在回复中…");
            return;
        }
        String slug = crush == null ? null : crush.slug;
        Ui.toast(requireContext(), "军师正在处理「" + c.title + "」…");
        SkillApi.invoke(c.name, "", crush == null || !c.requiresCrush ? null : slug,
                new Rest.Callback<String>() {
                    @Override
                    public void ok(String data) {
                        append(data == null ? "" : data);
                    }

                    @Override
                    public void fail(String message) {
                        Ui.toast(requireContext(), message, true);
                    }
                });
    }

    private void ask(String text) {
        if (streaming) {
            return;
        }
        input.setText("");
        append("我的问题：" + text);
        streaming = true;
        String slug = crush == null ? null : crush.slug;
        final StringBuilder sb = new StringBuilder();
        ChatApi.streamAdvisor(slug, text, null, new Sse.Listener() {
            @Override
            public void onEvent(String data) {
                try {
                    MultiChunk c = GsonFactory.GSON.fromJson(data, MultiChunk.class);
                    if ("sticker".equals(c.type)) {
                        sb.append("[表情包]");
                    } else {
                        sb.append(c.content);
                    }
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onClosed() {
                streaming = false;
                flush(sb);
            }

            @Override
            public void onError(String message) {
                streaming = false;
                flush(sb);
                Ui.toast(requireContext(), message);
            }
        });
    }

    private void append(String line) {
        log.add(line);
        adapter.notifyItemInserted(log.size() - 1);
        scrollBottom();
        // 新气泡入场微动画
        if (layoutManager != null && log.size() > 0) {
            View v = layoutManager.findViewByPosition(log.size() - 1);
            if (v != null) {
                Ui.enter(v, R.anim.item_fade_slide);
            }
        }
    }

    private void flush(StringBuilder sb) {
        if (sb.length() > 0) {
            append(sb.toString());
            sb.setLength(0);
        }
    }

    private void scrollBottom() {
        if (layoutManager != null && log.size() > 0) {
            layoutManager.scrollToPosition(log.size() - 1);
        }
    }

    private class AdvisorMsgAdapter extends RecyclerView.Adapter<AdvisorMsgAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            final TextView tv;

            Holder(View v) {
                super(v);
                tv = (TextView) v;
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(15);
            tv.setPadding(Ui.dp(parent.getContext(), 12), Ui.dp(parent.getContext(), 9),
                    Ui.dp(parent.getContext(), 12), Ui.dp(parent.getContext(), 9));
            tv.setMaxWidth(Ui.dp(parent.getContext(), 270));
            tv.setLineSpacing(Ui.dp(parent.getContext(), 3), 1f);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(parent.getContext(), 6);
            tv.setLayoutParams(lp);
            return new Holder(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            String line = log.get(position);
            boolean mine = line.startsWith("我的问题：");
            h.tv.setText(line);
            h.tv.setGravity(mine ? Gravity.END : Gravity.START);
            h.tv.setBackgroundResource(mine ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_assistant);
            h.tv.setTextColor(mine ? 0xFFFFFFFF : 0xFF332A35);
            h.tv.setMaxWidth(Ui.dp(h.itemView.getContext(), mine ? 210 : 270));
        }

        @Override
        public int getItemCount() {
            return log.size();
        }
    }
}