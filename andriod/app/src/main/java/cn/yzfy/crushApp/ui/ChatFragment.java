package cn.yzfy.crushApp.ui;

import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.ChatApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.ImageLoader;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.Sse;
import cn.yzfy.crushApp.api.VoiceApi;
import cn.yzfy.crushApp.dto.ChatRequest;
import cn.yzfy.crushApp.model.ChatHistory;
import cn.yzfy.crushApp.model.ChatMessage;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.MultiChunk;

/** 聊天页：SSE 流式多气泡 + 图片 + 主动消息监听 + 语音 */
public class ChatFragment extends Fragment {

    private final List<ChatMessage> messages = new ArrayList<>();
    private Crush crush;
    private RecyclerView list;
    private ChatAdapter adapter;
    private LinearLayoutManager layoutManager;
    private EditText input;
    private TextView attachBtn;
    private TextView sendBtn;
    private LinearLayout previewRow;
    private ImageView previewThumb;
    private TextView previewLabel;
    private TextView statusText;

    private final SparseArray<ChatMessage> streamBubbles = new SparseArray<>();
    private boolean streaming;
    private Sse.Handle chatHandle;
    private Sse.Handle listenHandle;

    private String pickedBase64;
    private String pickedMime;
    private MediaPlayer player;

    private ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    private static final Handler UI = new Handler(Looper.getMainLooper());

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
        header.setPadding(Ui.dp(ctx, 6), Ui.dp(ctx, 8), Ui.dp(ctx, 6), Ui.dp(ctx, 8));
        header.setElevation(Ui.dp(ctx, 2));
        root.addView(header);

        TextView back = new TextView(ctx);
        back.setText("‹");
        back.setTextSize(32);
        back.setTextColor(0xFF4A4052);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> requireActivity().onBackPressed());
        Ui.pressScale(back);
        header.addView(back, Ui.dp(ctx, 44), Ui.dp(ctx, 44));

        LinearLayout nameCol = new LinearLayout(ctx);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams nclp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nameCol.setLayoutParams(nclp);
        TextView name = new TextView(ctx);
        name.setText(crush == null ? "聊一聊" : crush.name);
        name.setTextSize(17);
        name.setTextColor(0xFF2A2233);
        name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        nameCol.addView(name);
        TextView sub = new TextView(ctx);
        sub.setText(crush == null ? "" : crush.stageLabel());
        sub.setTextSize(11);
        sub.setTextColor(0xFFA5929C);
        nameCol.addView(sub);
        header.addView(nameCol);

        statusText = new TextView(ctx);
        statusText.setText("·");
        statusText.setTextSize(11);
        statusText.setTextColor(0xFFA5929C);
        header.addView(statusText);

        TextView waitBtn = new TextView(ctx);
        waitBtn.setText("等TA找我");
        waitBtn.setTextSize(13);
        waitBtn.setTextColor(0xFFFF5A7A);
        waitBtn.setGravity(Gravity.CENTER);
        waitBtn.setBackground(Ui.rounded(0xFFFFF0F3, 16));
        waitBtn.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        waitBtn.setOnClickListener(v -> proactive());
        Ui.pressScale(waitBtn);
        header.addView(waitBtn);

        // 消息列表
        list = new RecyclerView(ctx);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        list.setLayoutParams(llp);
        layoutManager = new LinearLayoutManager(ctx);
        layoutManager.setStackFromEnd(true);
        list.setLayoutManager(layoutManager);
        list.setClipToPadding(false);
        int pad = Ui.dp(ctx, 10);
        list.setPadding(pad, pad, pad, pad);
        adapter = new ChatAdapter();
        list.setAdapter(adapter);
        root.addView(list);

        root.addView(buildComposer(ctx));
        return root;
    }

    private View buildComposer(android.content.Context ctx) {
        LinearLayout shell = new LinearLayout(ctx);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(0xFFFFFFFF);
        shell.setElevation(Ui.dp(ctx, 3));
        shell.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 6), Ui.dp(ctx, 10), Ui.dp(ctx, 8));

        // 已选图片预览行
        previewRow = new LinearLayout(ctx);
        previewRow.setOrientation(LinearLayout.HORIZONTAL);
        previewRow.setGravity(Gravity.CENTER_VERTICAL);
        previewRow.setVisibility(View.GONE);
        previewThumb = new ImageView(ctx);
        previewThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        previewThumb.setBackground(Ui.rounded(0xFFF3E4E9, 10));
        previewRow.addView(previewThumb, Ui.dp(ctx, 44), Ui.dp(ctx, 44));
        previewLabel = new TextView(ctx);
        previewLabel.setTextSize(13);
        previewLabel.setTextColor(0xFF6B5E70);
        previewLabel.setPadding(Ui.dp(ctx, 8), 0, Ui.dp(ctx, 8), 0);
        previewRow.addView(previewLabel);
        TextView remove = new TextView(ctx);
        remove.setText("✕ 移除");
        remove.setTextSize(12);
        remove.setTextColor(0xFFFF4D6A);
        remove.setOnClickListener(v -> clearPicked());
        previewRow.addView(remove);
        shell.addView(previewRow);

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        attachBtn = new TextView(ctx);
        attachBtn.setText("📷 图片");
        attachBtn.setTextSize(14);
        attachBtn.setTextColor(0xFF6B5E70);
        attachBtn.setGravity(Gravity.CENTER);
        attachBtn.setBackground(Ui.rounded(0xFFFBF6F7, 14));
        attachBtn.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        attachBtn.setOnClickListener(v -> pickImage());
        Ui.pressScale(attachBtn);
        row.addView(attachBtn);

        input = new EditText(ctx);
        input.setHint("说点什么…");
        input.setTextSize(15);
        input.setTextColor(0xFF2A2233);
        input.setHintTextColor(0xFFC9B6BE);
        input.setBackground(Ui.rounded(0xFFFBF6F7, 14));
        input.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 8), Ui.dp(ctx, 12), Ui.dp(ctx, 8));
        input.setMaxLines(4);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ilp.leftMargin = Ui.dp(ctx, 8);
        ilp.rightMargin = Ui.dp(ctx, 8);
        input.setLayoutParams(ilp);
        row.addView(input);

        sendBtn = new TextView(ctx);
        sendBtn.setText("发送");
        sendBtn.setTextSize(15);
        sendBtn.setTextColor(0xFFFFFFFF);
        sendBtn.setGravity(Gravity.CENTER);
        sendBtn.setBackground(Ui.rounded(0xFFFF5A7A, 14));
        sendBtn.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 9), Ui.dp(ctx, 16), Ui.dp(ctx, 9));
        sendBtn.setOnClickListener(v -> send());
        Ui.pressScale(sendBtn);
        row.addView(sendBtn);
        shell.addView(row);
        return shell;
    }

    private void pickImage() {
        pickMediaLauncher.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pickMediaLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), uri -> {
                    if (uri == null) return;
                    handlePicked(uri);
                });
        loadHistory(true);
        startListen();
    }

    private void handlePicked(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            is.close();
            byte[] bytes = bos.toByteArray();
            pickedBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
            pickedMime = requireContext().getContentResolver().getType(uri);
            if (pickedMime == null) {
                pickedMime = "image/jpeg";
            }
            previewThumb.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
            previewLabel.setText(String.format("已选图片（%dKB）", bytes.length / 1024));
            previewRow.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Ui.toast(requireContext(), "读取图片失败，请换一张重试", FriendlyToast.Type.ERROR);
        }
    }

    private void clearPicked() {
        pickedBase64 = null;
        pickedMime = null;
        previewRow.setVisibility(View.GONE);
    }

    private void send() {
        if (streaming) {
            Ui.toast(requireContext(), "TA 正在回复中，稍等片刻…");
            return;
        }
        String text = input.getText().toString().trim();
        String imgBase64 = pickedBase64;
        String imgMime = pickedMime;
        if (text.isEmpty() && imgBase64 == null) {
            return;
        }
        input.setText("");
        clearPicked();
        appendMessage(userMsg(text, imgBase64, imgMime));

        List<ChatRequest.ChatMedia> media = new ArrayList<>();
        if (imgBase64 != null) {
            ChatRequest.ChatMedia m = new ChatRequest.ChatMedia("IMAGE_BASE64", imgMime, imgBase64,
                    System.currentTimeMillis() + ".img");
            media.add(m);
        }

        streaming = true;
        indicateStreaming();
        final String slug = crush == null ? "" : crush.slug;
        chatHandle = ChatApi.streamChat(slug, text, media, null, false, new Sse.Listener() {
            @Override
            public void onEvent(String data) {
                try {
                    MultiChunk c = GsonFactory.GSON.fromJson(data, MultiChunk.class);
                    onChunk(c);
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onClosed() {
                endStreaming("·");
            }

            @Override
            public void onError(String message) {
                endStreaming("·");
                Ui.toast(requireContext(), message, FriendlyToast.Type.ERROR);
            }
        });
    }

    private ChatMessage userMsg(String text, String base64, String mime) {
        if (base64 != null) {
            if (mime == null || mime.isEmpty()) {
                mime = "image/jpeg";
            }
            return ChatMessage.image(ChatMessage.Role.USER, "data:" + mime + ";base64," + base64);
        }
        return ChatMessage.text(ChatMessage.Role.USER, text);
    }

    private void onChunk(MultiChunk c) {
        ChatMessage m = streamBubbles.get(c.index);
        if (m == null) {
            m = new ChatMessage();
            m.role = ChatMessage.Role.ASSISTANT;
            if ("sticker".equals(c.type)) {
                m.kind = ChatMessage.Kind.STICKER;
                m.imageUrl = c.content;
            } else {
                m.kind = ChatMessage.Kind.TEXT;
                m.text = c.content == null ? "" : c.content;
            }
            streamBubbles.put(c.index, m);
            appendMessage(m);
        } else {
            if ("sticker".equals(c.type)) {
                m.imageUrl = c.content;
            } else {
                m.text += c.content;
            }
            adapter.notifyItemChanged(messages.indexOf(m));
        }
        m.pending = !c.done;
        if (c.done) {
            scrollBottom();
        }
    }

    private void appendMessage(ChatMessage m) {
        messages.add(m);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollBottom();
        // 新气泡入场微动画：淡入轻微上滑
        UI.post(() -> {
            if (layoutManager != null && messages.size() > 0) {
                View v = layoutManager.findViewByPosition(messages.size() - 1);
                if (v != null) {
                    Ui.enter(v, R.anim.item_fade_slide);
                }
            }
        });
    }

    private void scrollBottom() {
        UI.post(() -> {
            if (layoutManager != null && messages.size() > 0) {
                layoutManager.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void indicateStreaming() {
        statusText.setText("TA 正在输入…");
    }

    private void endStreaming(String label) {
        streaming = false;
        streamBubbles.clear();
        statusText.setText(label);
        scrollBottom();
    }

    /** 主动对话：让 TA 等不住先找你 */
    private void proactive() {
        if (streaming) {
            Ui.toast(requireContext(), "TA 正在回复中，稍等片刻…");
            return;
        }
        if (crush == null) {
            return;
        }
        Ui.toast(requireContext(), "正在撩 TA，等 TA 主动发消息…");
        final String slug = crush.slug;
        chatHandle = ChatApi.streamProactive(slug, "用户想看看你会不会主动来找我",
                new Sse.Listener() {
                    @Override
                    public void onEvent(String data) {
                        try {
                            MultiChunk c = GsonFactory.GSON.fromJson(data, MultiChunk.class);
                            onChunk(c);
                        } catch (Exception ignored) {
                        }
                    }

                    @Override
                    public void onClosed() {
                    }

                    @Override
                    public void onError(String message) {
                        Ui.toast(requireContext(), message);
                    }
                });
    }

    /** 常驻监听 crush 主动消息：收到后刷新历史 */
    private void startListen() {
        if (crush == null) {
            return;
        }
        listenHandle = ChatApi.listenProactive(crush.slug, new Sse.Listener() {
            @Override
            public void onEvent(String data) {
                if (!streaming) {
                    loadHistory(false);
                    Ui.toast(requireContext(), "💌「" + crush.name + "」主动发来消息",
                            FriendlyToast.Type.INFO, true, () -> {
                                if (getView() != null) {
                                    loadHistory(false);
                                    scrollBottom();
                                }
                            });
                }
            }

            @Override
            public void onClosed() {
            }

            @Override
            public void onError(String message) {
                // 连接断开会自动重连（下次触发）
            }
        });
    }

    private void loadHistory(final boolean reset) {
        if (crush == null) {
            return;
        }
        ChatApi.history(crush.slug, new Rest.Callback<List<ChatHistory>>() {
            @Override
            public void ok(List<ChatHistory> data) {
                if (reset || !streaming) {
                    messages.clear();
                    if (data != null) {
                        for (ChatHistory h : data) {
                            if ("user".equals(h.role) || "assistant".equals(h.role)) {
                                if (h.mediaUrl != null && !h.mediaUrl.isEmpty()) {
                                    messages.add(ChatMessage.image(
                                            "user".equals(h.role) ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT,
                                            h.mediaUrl));
                                }
                                if (h.content != null && !h.content.trim().isEmpty()) {
                                    messages.add(ChatMessage.text(
                                            "user".equals(h.role) ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT,
                                            h.content));
                                }
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    scrollBottom();
                }
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), "加载历史失败：" + message, FriendlyToast.Type.ERROR);
            }
        });
    }

    /** 把最新一条 assistant 文本合成为语音播放 */
    private void speakLast() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m.role == ChatMessage.Role.ASSISTANT && m.kind == ChatMessage.Kind.TEXT && !m.text.trim().isEmpty()) {
                speak(m.text);
                return;
            }
        }
        Ui.toast(requireContext(), "还没有可播放的回复", FriendlyToast.Type.WARN);
    }

    /** 播放中状态：正在播放的文本与开关标志（供语音按钮显示 ▶/■） */
    private boolean playingVoice = false;
    private String playingText = null;

    private void toggleVoice(String text) {
        if (playingVoice) {
            releasePlayer();
            return;
        }
        speak(text);
    }

    private void speak(String text) {
        Ui.toast(requireContext(), "TA 正在开口…", FriendlyToast.Type.INFO);
        VoiceApi.synthesize(text, VoiceApi.cachedVoice(), new Rest.Callback<String>() {
            @Override
            public void ok(String base64) {
                playingText = text;
                playMp3(base64);
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), "语音合成失败：" + message, FriendlyToast.Type.ERROR);
            }
        });
    }

    private void playMp3(final String base64) {
        UI.post(() -> {
            try {
                releasePlayer();
                File f = new File(requireContext().getCacheDir(), "voice_" + System.currentTimeMillis() + ".mp3");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(android.util.Base64.decode(base64, android.util.Base64.DEFAULT));
                fos.close();
                player = new MediaPlayer();
                player.setDataSource(f.getAbsolutePath());
                player.setOnCompletionListener(mp -> releasePlayer());
                player.setOnErrorListener((mp, w, e) -> {
                    releasePlayer();
                    return true;
                });
                player.prepare();
                player.start();
                playingVoice = true;
                playingText = textOfLastAssistant();
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Ui.toast(requireContext(), "播放失败，请稍后再试", FriendlyToast.Type.ERROR);
            }
        });
    }

    private String textOfLastAssistant() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatMessage m = messages.get(i);
            if (m.role == ChatMessage.Role.ASSISTANT && m.kind == ChatMessage.Kind.TEXT && !m.text.isEmpty()) {
                return m.text;
            }
        }
        return playingText;
    }

    private void releasePlayer() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
        if (playingVoice) {
            playingVoice = false;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onDestroyView() {
        if (chatHandle != null) chatHandle.close();
        if (listenHandle != null) listenHandle.close();
        releasePlayer();
        super.onDestroyView();
    }

    // ------------------------------------------------------------------
    // Adapter
    // ------------------------------------------------------------------
    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

        class Holder extends RecyclerView.ViewHolder {
            Holder(View v) {
                super(v);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(buildBubble(parent));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final ChatMessage m = messages.get(position);
            LinearLayout row = (LinearLayout) h.itemView;
            row.removeAllViews();
            row.setGravity(m.role == ChatMessage.Role.USER ? Gravity.END : Gravity.START);

            LinearLayout bubble = new LinearLayout(row.getContext());
            bubble.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            blp.leftMargin = Ui.dp(row.getContext(), m.role == ChatMessage.Role.USER ? 56 : 0);
            blp.rightMargin = Ui.dp(row.getContext(), m.role == ChatMessage.Role.USER ? 0 : 56);
            bubble.setGravity(m.role == ChatMessage.Role.USER ? Gravity.END : Gravity.START);
            bubble.setLayoutParams(blp);

            if (m.kind == ChatMessage.Kind.IMAGE) {
                ImageView img = new ImageView(row.getContext());
                img.setMaxWidth(Ui.dp(row.getContext(), 200));
                img.setMaxHeight(Ui.dp(row.getContext(), 200));
                img.setAdjustViewBounds(true);
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setOnClickListener(v -> previewImage(m.imageUrl));
                bubble.addView(img, new LinearLayout.LayoutParams(
                        Ui.dp(row.getContext(), 160), Ui.dp(row.getContext(), 160)));
                ImageLoader.load(img, m.imageUrl);
            } else if (m.kind == ChatMessage.Kind.STICKER) {
                ImageView img = new ImageView(row.getContext());
                img.setMaxWidth(Ui.dp(row.getContext(), 220));
                img.setMaxHeight(Ui.dp(row.getContext(), 220));
                img.setAdjustViewBounds(true);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                img.setOnClickListener(v -> previewImage(m.imageUrl));
                bubble.addView(img, new LinearLayout.LayoutParams(
                        Ui.dp(row.getContext(), 140), Ui.dp(row.getContext(), 140)));
                ImageLoader.load(img, m.imageUrl);
            } else {
                TextView tv = new TextView(row.getContext());
                tv.setTextSize(15);
                tv.setTextColor(m.role == ChatMessage.Role.USER ? 0xFFFFFFFF : 0xFF332A35);
                tv.setLineSpacing(Ui.dp(row.getContext(), 3), 1f);
                String body = m.text;
                if (m.pending && body.length() == 0) {
                    body = "…";
                } else if (m.pending) {
                    body = body + "▍";
                }
                tv.setText(body);
                tv.setMaxWidth(Ui.dp(row.getContext(), 260));
                tv.setPadding(Ui.dp(row.getContext(), 12), Ui.dp(row.getContext(), 9),
                        Ui.dp(row.getContext(), 12), Ui.dp(row.getContext(), 9));
                tv.setBackgroundResource(m.role == ChatMessage.Role.USER
                        ? R.drawable.bg_bubble_user : R.drawable.bg_bubble_assistant);
                tv.setOnLongClickListener(v -> {
                    if (m.role == ChatMessage.Role.ASSISTANT && m.kind == ChatMessage.Kind.TEXT && !m.text.isEmpty()) {
                        speak(m.text);
                    }
                    return true;
                });
                if (m.role == ChatMessage.Role.ASSISTANT && m.kind == ChatMessage.Kind.TEXT && !m.text.isEmpty()) {
                    // QQ 式语音播放按钮：点击播放/停止 TA 的这条回复
                    LinearLayout msgRow = new LinearLayout(row.getContext());
                    msgRow.setOrientation(LinearLayout.HORIZONTAL);
                    msgRow.setGravity(Gravity.CENTER_VERTICAL);

                    TextView playBtn = new TextView(row.getContext());
                    playBtn.setText(playingVoice && m.text.equals(playingText) ? "■" : "▶");
                    playBtn.setTextSize(13);
                    playBtn.setTextColor(0xFFFFFFFF);
                    playBtn.setGravity(Gravity.CENTER);
                    playBtn.setBackground(playingVoice && m.text.equals(playingText)
                            ? Ui.circle(0xFF2EC4B6) : Ui.circle(0xFF7256FF));
                    LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                            Ui.dp(row.getContext(), 34), Ui.dp(row.getContext(), 34));
                    playLp.rightMargin = Ui.dp(row.getContext(), 6);
                    playBtn.setLayoutParams(playLp);
                    Ui.pressScale(playBtn);
                    playBtn.setOnClickListener(v -> toggleVoice(m.text));
                    msgRow.addView(playBtn);

                    msgRow.addView(tv);
                    bubble.addView(msgRow);
                } else {
                    bubble.addView(tv);
                }
            }

            TextView time = new TextView(row.getContext());
            time.setText(m.timeLabel());
            time.setTextSize(10);
            time.setTextColor(0xFFB8A5AC);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Ui.dp(row.getContext(), 3);
            time.setLayoutParams(tlp);
            bubble.addView(time);

            row.addView(bubble);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }

        private View buildBubble(ViewGroup parent) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            RecyclerView.LayoutParams rp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rp.topMargin = Ui.dp(parent.getContext(), 6);
            rp.bottomMargin = Ui.dp(parent.getContext(), 6);
            row.setLayoutParams(rp);
            return row;
        }
    }

    private void previewImage(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        android.app.Dialog d = new android.app.Dialog(requireContext());
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        ImageView img = new ImageView(requireContext());
        img.setBackgroundColor(0xEE000000);
        img.setScaleType(ImageView.ScaleType.FIT_CENTER);
        img.setOnClickListener(v -> d.dismiss());
        d.setContentView(img, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
        ImageLoader.load(img, url);
    }
}