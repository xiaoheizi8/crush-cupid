package cn.yzfy.crushApp.ui;

import android.animation.ObjectAnimator;
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

import io.noties.markwon.Markwon;

import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.ChatApi;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.ImageLoader;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.SkillApi;
import cn.yzfy.crushApp.api.Sse;
import cn.yzfy.crushApp.model.AdvisorCommand;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.MultiChunk;

/**
 * 军师页：子命令卡片 + 自由对话（独立记忆，流式渲染）
 */
public class AdvisorFragment extends Fragment {

    /**
     * 一条气泡消息；按对象字段区分归属，不再靠「我的问题：」前缀猜
     */
    private static class Msg {
        final boolean mine;
        String text;
        boolean pending;
        /**
         * REST 调用中的「军师思考中」占位态（呼吸动画 + 失败时移除）
         */
        boolean thinking;
        /**
         * 表情包图片地址（sticker chunk 的 URL），与 ChatFragment 的 STICKER 气泡对齐
         */
        String imageUrl;

        Msg(boolean mine, String text) {
            this.mine = mine;
            this.text = text;
        }
    }

    private final List<Msg> msgs = new ArrayList<>();
    private Crush crush;
    private TextView crushChip;
    private List<Crush> crushes = new ArrayList<>();
    private RecyclerView list;
    private AdvisorMsgAdapter adapter;
    private LinearLayoutManager layoutManager;
    private EditText input;
    private boolean streaming;
    private Sse.Handle advisorHandle;
    /**
     * Markdown 渲染器：军师报告类输出按 markdown 展示，Fragment 内复用一个实例
     */
    private Markwon markwon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();
        crush = HomeFragment.crushFrom(getArguments());
        markwon = Markwon.create(ctx);

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

        // 分析对象选择器：军师需要绑定一个暗恋对象，否则 report/strategy 等命令无法执行
        TextView crushChip = new TextView(ctx);
        crushChip.setText(crushText());
        crushChip.setTextSize(12);
        crushChip.setTextColor(0xFF7256FF);
        crushChip.setGravity(Gravity.CENTER);
        crushChip.setBackground(Ui.rounded(0xFFEFEBFF, 999));
        crushChip.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        crushChip.setOnClickListener(v -> pickCrush(null));
        Ui.pressScale(crushChip);
        this.crushChip = crushChip;

        LinearLayout nc2 = new LinearLayout(ctx);
        nc2.setOrientation(LinearLayout.VERTICAL);
        nc2.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(nc2);
        nc2.addView(crushChip, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
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
                if (!isAdded()) {
                    return; // 已离开本页
                }
                cmdRow.removeAllViews();
                if (data == null) {
                    return;
                }
                int shown = 0;
                for (final AdvisorCommand c : data) {
                    TextView chip = new TextView(requireContext());
                    chip.setText((c.icon == null || c.icon.isEmpty() ? "▫️ " : c.icon + " ") + c.title);
                    chip.setTextSize(14);
                    // 分组着色：军师=紫 / 照镜子=琥珀金 / 模拟器=粉
                    int[] theme = chipTheme(c.group);
                    chip.setTextColor(theme[1]);
                    chip.setGravity(Gravity.CENTER);
                    chip.setBackground(Ui.rounded(theme[0], 999));
                    chip.setPadding(Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 8),
                            Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 8));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.rightMargin = Ui.dp(requireContext(), 8);
                    chip.setLayoutParams(lp);
                    chip.setOnClickListener(v -> invoke(c));
                    Ui.pressScale(chip);
                    cmdRow.addView(chip);
                    // 逐个入场：淡入 + 右侧滑入，间隔 40ms
                    chip.setAlpha(0f);
                    chip.setTranslationX(Ui.dp(requireContext(), 18));
                    chip.animate().alpha(1f).translationX(0f)
                            .setStartDelay(60L * shown)
                            .setDuration(260L)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator())
                            .start();
                    shown++;
                }
            }

            @Override
            public void fail(String message) {
                if (isAdded()) {
                    Ui.toast(AdvisorFragment.this, "加载军师功能失败：" + message, FriendlyToast.Type.ERROR, true);
                }
            }
        });
    }

    /**
     * 分组配色：{背景色, 文字色}；未知分组回落军师紫
     */
    private int[] chipTheme(String group) {
        if ("MIRROR".equals(group)) {
            return new int[]{0xFFFFF3D6, 0xFFA9761B};
        }
        if ("SIMULATOR".equals(group)) {
            return new int[]{0xFFFFE9EE, 0xFFE8466B};
        }
        return new int[]{0xFFEFEBFF, 0xFF7256FF};
    }

    private String crushText() {
        return "♡ 对象：" + (crush == null ? "未选择，点此选择" : crush.name);
    }

    private void refreshCrushChip() {
        if (crushChip != null) {
            crushChip.setText(crushText());
        }
    }

    private void pickCrush(final Runnable afterPick) {
        CrushApi.list(new Rest.Callback<List<Crush>>() {
            @Override
            public void ok(List<Crush> data) {
                if (!isAdded()) {
                    return; // 已离开页面，丢弃回调
                }
                crushes = data == null ? new ArrayList<>() : data;
                if (crushes.isEmpty()) {
                    Ui.toast(AdvisorFragment.this, "还没有暗恋对象，先去新建一个吧", FriendlyToast.Type.WARN);
                    return;
                }
                String[] names = new String[crushes.size()];
                int[] checked = {-1};
                for (int i = 0; i < crushes.size(); i++) {
                    names[i] = crushes.get(i).name;
                    if (crush != null && crush.id != null && crush.id.equals(crushes.get(i).id)) {
                        checked[0] = i;
                    }
                }
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("选择分析的暗恋对象")
                        .setSingleChoiceItems(names, checked[0], (d, which) -> checked[0] = which)
                        .setPositiveButton("确定", (d, w) -> {
                            int i = checked[0];
                            if (i < 0 || i >= crushes.size()) {
                                return;
                            }
                            crush = crushes.get(i);
                            refreshCrushChip();
                            Ui.toast(AdvisorFragment.this, "已切换对象：" + crush.name, FriendlyToast.Type.SUCCESS);
                            if (afterPick != null) {
                                afterPick.run();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void fail(String message) {
                if (isAdded()) {
                    Ui.toast(AdvisorFragment.this, "加载对象失败：" + message, FriendlyToast.Type.ERROR);
                }
            }
        });
    }

    private void invoke(AdvisorCommand c) {
        if (streaming) {
            Ui.toast(AdvisorFragment.this, "军师正在回复中…", FriendlyToast.Type.INFO);
            return;
        }
        if (c.requiresCrush && crush == null) {
            Ui.toast(AdvisorFragment.this, "该功能需要先选择暗恋对象", FriendlyToast.Type.INFO);
            pickCrush(() -> startInvoke(c, null));
            return;
        }
        startInvoke(c, null);
    }

    /**
     * 命令调度：需要材料的命令先弹输入框（选完对象后再进来也走这里）
     */
    private void startInvoke(AdvisorCommand c, String input) {
        if (c.needsInput && input == null) {
            showInput(c);
            return;
        }
        invokeDirect(c, input == null ? "" : input);
    }

    /**
     * 需要用户粘贴材料（聊天记录/草稿/场景描述）的命令，弹出输入对话框
     */
    private void showInput(final AdvisorCommand c) {
        android.content.Context ctx = requireContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 22), Ui.dp(ctx, 6), Ui.dp(ctx, 22), 0);

        TextView hint = new TextView(ctx);
        hint.setText(c.description);
        hint.setTextSize(13);
        hint.setTextColor(0xFF8A7A8F);
        hint.setLineSpacing(Ui.dp(ctx, 2), 1f);
        box.addView(hint);

        final EditText et = new EditText(ctx);
        et.setHint(c.inputHint == null ? "输入内容…" : c.inputHint);
        et.setTextSize(14);
        et.setTextColor(0xFF2A2233);
        et.setMinLines(3);
        et.setMaxLines(8);
        et.setGravity(Gravity.TOP);
        et.setBackground(Ui.rounded(0xFFF6F3FF, 14));
        et.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 10), Ui.dp(ctx, 12), Ui.dp(ctx, 10));
        LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        elp.topMargin = Ui.dp(ctx, 10);
        et.setLayoutParams(elp);
        box.addView(et);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(ctx)
                .setTitle((c.icon == null ? "" : c.icon + " ") + c.title)
                .setView(box)
                .setPositiveButton("交给军师", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (text.isEmpty()) {
                        Ui.toast(AdvisorFragment.this, "先写点材料，军师才好出手", FriendlyToast.Type.WARN);
                        return;
                    }
                    invokeDirect(c, text);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void invokeDirect(AdvisorCommand c, String question) {
        streaming = true;
        String slug = crush == null ? null : crush.slug;
        // 占位「思考中」气泡：呼吸动画，失败时移除，成功时原地替换为结果
        final int idx = msgs.size();
        Msg placeholder = new Msg(false, "");
        placeholder.pending = true;
        placeholder.thinking = true;
        addMsg(placeholder);
        SkillApi.invoke(c.name, question, crush == null || !c.requiresCrush ? null : slug,
                new Rest.Callback<String>() {
                    @Override
                    public void ok(String data) {
                        streaming = false;
                        if (idx >= msgs.size()) {
                            return;
                        }
                        Msg m = msgs.get(idx);
                        m.pending = false;
                        m.thinking = false;
                        String body = data == null ? "" : data.trim();
                        m.text = body.isEmpty() ? "（军师没有返回内容，稍后再试试）" : body;
                        adapter.notifyItemChanged(idx);
                        scrollBottom();
                        // 结果就位轻触反馈
                        list.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                        Ui.post(() -> {
                            if (layoutManager != null) {
                                View v = layoutManager.findViewByPosition(idx);
                                if (v != null) {
                                    Ui.enter(v, R.anim.item_fade_slide);
                                }
                            }
                        });
                    }

                    @Override
                    public void fail(String message) {
                        streaming = false;
                        if (idx < msgs.size() && msgs.get(idx).thinking) {
                            msgs.remove(idx);
                            adapter.notifyItemRemoved(idx);
                        }
                        // 用户可能已离开本页：detached 后 requireContext() 会崩
                        if (isAdded()) {
                            Ui.toast(AdvisorFragment.this, message, FriendlyToast.Type.ERROR, true);
                        }
                    }
                });
    }

    private void ask(String text) {
        if (streaming) {
            return;
        }
        if (crush == null) {
            Ui.toast(AdvisorFragment.this, "为获得更懂 TA 的答复，请先选择暗恋对象", FriendlyToast.Type.INFO);
            pickCrush(() -> askDirect(text));
            return;
        }
        askDirect(text);
    }

    private void askDirect(String text) {
        input.setText("");
        addMsg(new Msg(true, text));
        streaming = true;
        String slug = crush == null ? null : crush.slug;
        closeAdvisor();
        // 先放一条空回复占位，后续 chunk 原地更新 → 打字机效果
        final int replyIdx = msgs.size();
        Msg reply = new Msg(false, "");
        reply.pending = true;
        addMsg(reply);
        advisorHandle = ChatApi.streamAdvisor(slug, text, null, new Sse.Listener() {
            @Override
            public void onEvent(String data) {
                try {
                    MultiChunk c = GsonFactory.GSON.fromJson(data, MultiChunk.class);
                    Msg m = msgs.get(replyIdx);
                    if ("sticker".equals(c.type)) {
                        // URL 表情包按图片渲染；情绪词等非 URL 内容降级为文本标记
                        String url = c.content == null ? "" : c.content.trim();
                        if (url.startsWith("http") || url.startsWith("/api/")) {
                            m.imageUrl = url;
                        } else {
                            m.text += "（表情包）";
                        }
                    } else if (c.content != null) {
                        m.text += c.content;
                    }
                    adapter.notifyItemChanged(replyIdx);
                    scrollBottom();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onClosed() {
                finishReply(replyIdx, null);
            }

            @Override
            public void onError(String message) {
                finishReply(replyIdx, message);
                if (isAdded()) {
                    Ui.toast(AdvisorFragment.this, message, FriendlyToast.Type.ERROR);
                }
            }
        });
    }

    /**
     * 流结束：去掉光标；空回复/中断给兜底文案
     */
    private void finishReply(int idx, String error) {
        streaming = false;
        if (idx >= msgs.size()) {
            return;
        }
        Msg m = msgs.get(idx);
        m.pending = false;
        boolean hasImage = m.imageUrl != null && !m.imageUrl.isEmpty();
        if (m.text.trim().isEmpty() && !hasImage) {
            // 文本与表情包都为空才算空回复
            m.text = error == null ? "（军师没有返回内容，稍后再试试）" : "（出错了：" + error + "）";
        } else if (error != null) {
            m.text += "\n（连接中断：" + error + "）";
        }
        adapter.notifyItemChanged(idx);
        scrollBottom();
    }

    /**
     * 关闭当前军师流（SSE 句柄防覆盖泄漏），同 ChatFragment.closeStream
     */
    private void closeAdvisor() {
        if (advisorHandle != null) {
            advisorHandle.close();
            advisorHandle = null;
        }
    }

    @Override
    public void onDestroyView() {
        closeAdvisor();
        super.onDestroyView();
    }

    private void addMsg(Msg m) {
        msgs.add(m);
        adapter.notifyItemInserted(msgs.size() - 1);
        scrollBottom();
        // 新气泡入场微动画：布局完成后查询新 view 再播放
        Ui.post(() -> {
            if (layoutManager != null && msgs.size() > 0) {
                View v = layoutManager.findViewByPosition(msgs.size() - 1);
                if (v != null) {
                    Ui.enter(v, R.anim.item_fade_slide);
                }
            }
        });
    }

    private void scrollBottom() {
        if (layoutManager != null && msgs.size() > 0) {
            layoutManager.scrollToPosition(msgs.size() - 1);
        }
    }

    private class AdvisorMsgAdapter extends RecyclerView.Adapter<AdvisorMsgAdapter.Holder> {
        class Holder extends RecyclerView.ViewHolder {
            final LinearLayout box;
            final TextView tv;
            final android.widget.ImageView img;
            ObjectAnimator breathe;

            Holder(View v) {
                super(v);
                box = (LinearLayout) v;
                tv = (TextView) box.getChildAt(0);
                img = (android.widget.ImageView) box.getChildAt(1);
            }

            /**
             * 「军师思考中」呼吸动画
             */
            void startBreathe() {
                stopBreathe();
                breathe = ObjectAnimator.ofFloat(box, View.ALPHA, 1f, 0.4f);
                breathe.setDuration(700);
                breathe.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                breathe.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                breathe.start();
            }

            void stopBreathe() {
                if (breathe != null) {
                    breathe.cancel();
                    breathe = null;
                }
                box.setAlpha(1f);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.content.Context ctx = parent.getContext();
            // 气泡容器：支持「文本 / 表情包图片 / 图文」三种内容
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 9), Ui.dp(ctx, 12), Ui.dp(ctx, 9));
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(ctx, 6);
            box.setLayoutParams(lp);

            TextView tv = new TextView(ctx);
            tv.setTextSize(15);
            tv.setLineSpacing(Ui.dp(ctx, 3), 1f);
            tv.setMaxWidth(Ui.dp(ctx, 270));
            box.addView(tv);

            android.widget.ImageView img = new android.widget.ImageView(ctx);
            img.setVisibility(View.GONE);
            img.setAdjustViewBounds(true);
            img.setMaxWidth(Ui.dp(ctx, 200));
            img.setMaxHeight(Ui.dp(ctx, 200));
            img.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            box.addView(img);
            return new Holder(box);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            Msg m = msgs.get(position);
            h.stopBreathe();
            boolean sticker = !m.mine && m.imageUrl != null && !m.imageUrl.isEmpty();
            boolean hasText = m.text != null && !m.text.trim().isEmpty();

            h.box.setGravity(m.mine ? Gravity.END : Gravity.START);
            h.box.setBackgroundResource(m.mine ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_assistant);

            if (m.thinking) {
                h.img.setVisibility(View.GONE);
                h.tv.setVisibility(View.VISIBLE);
                h.tv.setText("🤵 军师思考中…");
                h.startBreathe();
            } else {
                if (sticker) {
                    h.img.setVisibility(View.VISIBLE);
                    h.img.setOnClickListener(v -> previewSticker(m.imageUrl));
                    ImageLoader.load(h.img, m.imageUrl);
                } else {
                    h.img.setVisibility(View.GONE);
                }
                // 只发图不带文时隐藏文本行
                h.tv.setVisibility(hasText ? View.VISIBLE : View.GONE);
                if (hasText) {
                    if (m.pending) {
                        // SSE 流式中的打字机光标
                        h.tv.setText(m.text.isEmpty() ? "…" : m.text + "▍");
                    } else if (!m.mine && markwon != null) {
                        // 军师输出常为 markdown（报告/分点策略），按 markdown 渲染
                        markwon.setMarkdown(h.tv, m.text);
                    } else {
                        h.tv.setText(m.text);
                    }
                }
            }
            h.tv.setTextColor(m.mine ? 0xFFFFFFFF : 0xFF332A35);
            h.tv.setMaxWidth(Ui.dp(h.itemView.getContext(), m.mine ? 210 : 270));
        }

        @Override
        public void onViewRecycled(@NonNull Holder holder) {
            super.onViewRecycled(holder);
            holder.stopBreathe();
        }

        @Override
        public int getItemCount() {
            return msgs.size();
        }
    }

    /**
     * 表情包点击放大预览（与聊天页图片预览交互一致）
     */
    private void previewSticker(String url) {
        if (url == null || url.isEmpty() || !isAdded()) {
            return;
        }
        android.app.Dialog d = new android.app.Dialog(requireContext());
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        android.widget.ImageView img = new android.widget.ImageView(requireContext());
        img.setBackgroundColor(0xEE000000);
        img.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        img.setOnClickListener(v -> d.dismiss());
        d.setContentView(img, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
        ImageLoader.load(img, url);
    }
}