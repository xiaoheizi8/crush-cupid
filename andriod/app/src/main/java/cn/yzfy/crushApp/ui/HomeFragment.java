package cn.yzfy.crushApp.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.AuthApi;
import cn.yzfy.crushApp.api.CrushApi;
import cn.yzfy.crushApp.api.GsonFactory;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.VoiceApi;
import cn.yzfy.crushApp.model.Crush;
import cn.yzfy.crushApp.model.User;

/** 首页：暗恋对象列表 + 新建 */
public class HomeFragment extends Fragment {

    public static final String EXTRA_CRUSH = "crush";

    private final List<Crush> items = new ArrayList<>();
    private HomeAdapter adapter;
    private TextView empty;
    private boolean animatedOnce;
    private RecyclerView list;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout refresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(0xFFFBF3F5);

        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 顶部渐变标题
        LinearLayout header = new LinearLayout(requireContext());
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(Ui.dp(requireContext(), 20), Ui.dp(requireContext(), 24),
                Ui.dp(requireContext(), 20), Ui.dp(requireContext(), 20));
        header.setBackgroundResource(R.drawable.bg_header_gradient);
        col.addView(header);

        TextView title = new TextView(requireContext());
        title.setText("Cupid ♥ 恋爱模拟");
        title.setTextSize(24);
        title.setTextColor(0xFFE8405F);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("把心动写进剧本，让 AI 陪你奔赴每一段浪漫");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFFB07B8C);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(requireContext(), 6);
        subtitle.setLayoutParams(slp);
        header.addView(subtitle);

        // 快捷入口（可横滑，避免小屏溢出）
        HorizontalScrollView quickScroll = new HorizontalScrollView(requireContext());
        quickScroll.setHorizontalScrollBarEnabled(false);
        quickScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout.LayoutParams qslp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qslp.topMargin = Ui.dp(requireContext(), 12);
        quickScroll.setLayoutParams(qslp);

        LinearLayout quick = new LinearLayout(requireContext());
        quick.setOrientation(LinearLayout.HORIZONTAL);
        quickScroll.addView(quick);
        header.addView(quickScroll);
        quick.addView(quickChip("🤵 军师", () -> Nav.push(requireActivity(), AdvisorFragment.class, null)));
        quick.addView(quickChip("📦 技能包", () -> Nav.push(requireActivity(), SkillCatalogFragment.class, null)));
        quick.addView(quickChip("🧠 模型", () -> Nav.push(requireActivity(), ProviderFragment.class, null)));
        quick.addView(quickChip("🎙 音色", () -> Nav.push(requireActivity(), VoiceConfigFragment.class, null)));
        quick.addView(quickChip("🚪 登出", this::logout));

        // 列表
        list = new RecyclerView(requireContext());
        FrameLayout.LayoutParams llp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        list.setLayoutParams(llp);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new HomeAdapter();
        list.setAdapter(adapter);
        // 下拉刷新
        refresh = Ui.pullRefresh(requireContext(), list, this::load);
        FrameLayout.LayoutParams rlp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        refresh.setLayoutParams(rlp);
        col.addView(refresh);

        // 空态
        empty = new TextView(requireContext());
        empty.setText("还没有暗恋对象…\n点击右下角的 + 新建一个吧");
        empty.setTextSize(14);
        empty.setTextColor(0xFFB8A5AC);
        empty.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams elp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        elp.topMargin = Ui.dp(requireContext(), 80);
        empty.setLayoutParams(elp);
        col.addView(empty);

        root.addView(col);

        FloatingActionButton fab = new FloatingActionButton(requireContext());
        fab.setImageResource(android.R.drawable.ic_input_add);
        fab.setContentDescription("新建暗恋对象");
        FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(
                Ui.dp(requireContext(), 56), Ui.dp(requireContext(), 56));
        flp.gravity = Gravity.BOTTOM | Gravity.END;
        flp.setMargins(0, 0, Ui.dp(requireContext(), 20), Ui.dp(requireContext(), 28));
        fab.setLayoutParams(flp);
        fab.setOnClickListener(v -> Nav.push(requireActivity(), CrushEditFragment.class, null));
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        root.addView(fab);

        Ui.listEnter(list, R.anim.layout_list);
        fab.animate().scaleX(1f).scaleY(1f).setStartDelay(180).setDuration(320)
                .setInterpolator(new android.view.animation.OvershootInterpolator(1.4f)).start();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
        syncVoicePref();
    }

    /** 从服务端同步偏好音色到本地缓存 */
    private void syncVoicePref() {
        AuthApi.me(new RestCallback<User>(u -> {
            if (u != null) {
                VoiceApi.cacheVoice(u.preferredVoice);
            }
        }, e -> {
            // 静默失败，网络问题不阻塞首页
        }));
    }

    private TextView quickChip(String label, Runnable action) {
        TextView t = new TextView(requireContext());
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(0xFF7256FF);
        t.setBackground(Ui.rounded(0xFFEFEBFF, 999));
        t.setPadding(Ui.dp(requireContext(), 10), Ui.dp(requireContext(), 6),
                Ui.dp(requireContext(), 10), Ui.dp(requireContext(), 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = Ui.dp(requireContext(), 8);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> action.run());
        Ui.pressScale(t);
        return t;
    }

    private void load() {
        CrushApi.list(new RestCallback<>(list -> {
            items.clear();
            if (list != null) items.addAll(list);
            adapter.notifyDataSetChanged();
            if (!animatedOnce) {
                animatedOnce = true;
                this.list.scheduleLayoutAnimation();
            }
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            if (refresh != null) refresh.setRefreshing(false);
        }, e -> {
            if (refresh != null) refresh.setRefreshing(false);
            Ui.toast(requireContext(), e, FriendlyToast.Type.ERROR, true);
        }));
    }

    /** 登出：走服务端登出，清 token 后回到登录页 */
    private void logout() {
        Ui.confirm(requireContext(), "登出", "确定要退出当前账号吗？", "登出", () -> {
            AuthApi.logout(new RestCallback<>(v -> Ui.post(() -> {
                AuthApi.clearToken();
                Ui.toast(requireContext(), "已安全退出，期待与你再会 ♥", FriendlyToast.Type.SUCCESS);
                Nav.reset(requireActivity(), new cn.yzfy.crushApp.ui.LoginFragment());
            }), e -> Ui.toast(requireContext(), e)));
        });
    }

    private class RestCallback<T> implements cn.yzfy.crushApp.api.Rest.Callback<T> {
        private final Consumer<T> ok;
        private final Consumer<String> err;

        RestCallback(Consumer<T> ok, Consumer<String> err) {
            this.ok = ok;
            this.err = err;
        }

        @Override
        public void ok(T data) {
            ok.accept(data);
        }

        @Override
        public void fail(String message) {
            err.accept(message);
        }
    }

    private interface Consumer<T> {
        void accept(T t);
    }

    private class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.Holder> {
        private final int[] PALETTE = {0xFFFF5A7A, 0xFF7256FF, 0xFFFF8FA3,
                0xFF5A9BFF, 0xFFFFB35A, 0xFF2FBF71};

        class Holder extends RecyclerView.ViewHolder {
            Holder(View v) {
                super(v);
            }
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(buildRow(parent));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder h, int position) {
            final Crush c = items.get(position);
            LinearLayout row = (LinearLayout) h.itemView;
            TextView avatar = (TextView) row.getChildAt(0);
            avatar.setText(c.initial());
            avatar.setBackground(Ui.circle(PALETTE[Math.abs(c.name == null ? 0 : c.name.hashCode()) % PALETTE.length]));
            LinearLayout col = (LinearLayout) row.getChildAt(1);
            TextView name = (TextView) col.getChildAt(0);
            name.setText(c.name);
            TextView sub = (TextView) col.getChildAt(1);
            StringBuilder s = new StringBuilder();
            if (c.stageLabel() != null && !c.stageLabel().isEmpty()) {
                s.append(c.stageLabel());
            }
            if (c.totalMessages != null) {
                s.append(s.length() > 0 ? " · " : "").append(c.totalMessages).append(" 条消息");
            }
            if (c.impression != null && !c.impression.trim().isEmpty()) {
                s.append(s.length() > 0 ? " · " : "").append(c.impression.trim());
            }
            sub.setText(s.length() == 0 ? "还没有聊过天" : s.toString());

            row.setOnClickListener(v ->
                    Nav.push(requireActivity(), ChatFragment.class, crushArgs(c)));
            row.setOnLongClickListener(v -> {
                showMenu(c);
                return true;
            });
            // 行内「编辑」按钮（子视图索引 2：avatar=0, col=1, edit=2, chevron=3）
            TextView editBtn = (TextView) row.getChildAt(2);
            editBtn.setOnClickListener(v ->
                    Nav.push(requireActivity(), CrushEditFragment.class, crushArgs(c)));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private View buildRow(ViewGroup parent) {
            android.content.Context ctx = parent.getContext();
            LinearLayout row = new LinearLayout(ctx);
            int pad = Ui.dp(ctx, 14);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(Ui.rounded(0xFFFFFFFF, 18));
            row.setElevation(Ui.dp(ctx, 1));
            row.setPadding(pad, Ui.dp(ctx, 14), pad, Ui.dp(ctx, 14));
            Ui.ripple(row);

            RecyclerView.LayoutParams rp = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int margin = Ui.dp(ctx, 6);
            rp.setMargins(margin, margin, margin, margin);
            row.setLayoutParams(rp);

            TextView avatar = new TextView(ctx);
            avatar.setTextColor(0xFFFFFFFF);
            avatar.setGravity(Gravity.CENTER);
            avatar.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            avatar.setTextSize(18);
            row.addView(avatar, Ui.dp(ctx, 52), Ui.dp(ctx, 52));

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setPadding(Ui.dp(ctx, 12), 0, 0, 0);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            col.setLayoutParams(clp);

            TextView name = new TextView(ctx);
            name.setTextSize(17);
            name.setTextColor(0xFF2A2233);
            name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            col.addView(name);

            TextView sub = new TextView(ctx);
            sub.setTextSize(12);
            sub.setTextColor(0xFFA5929C);
            LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            slp.topMargin = Ui.dp(ctx, 4);
            sub.setLayoutParams(slp);
            col.addView(sub);

            row.addView(col);

            // 行内「编辑」入口：与 web 端每行的「编辑」链接对齐，让新增后可直接改属性（含音色）
            TextView edit = new TextView(ctx);
            edit.setText("编辑");
            edit.setTextSize(12);
            edit.setTextColor(0xFFFF5A7A);
            edit.setGravity(Gravity.CENTER);
            edit.setBackground(Ui.rounded(0xFFFFF0F3, 999));
            edit.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
            LinearLayout.LayoutParams elp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            elp.leftMargin = Ui.dp(ctx, 8);
            edit.setLayoutParams(elp);
            Ui.pressScale(edit);
            row.addView(edit);

            TextView chevron = new TextView(ctx);
            chevron.setText("›");
            chevron.setTextSize(26);
            chevron.setTextColor(0xFFE3CDD4);
            row.addView(chevron);
            return row;
        }
    }

    private void showMenu(Crush c) {
        String[] labels = {"进入聊天", "查看资料", "编辑信息", "删除"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(c.name)
                .setItems(labels, (d, w) -> {
                    if (w == 0) {
                        Nav.push(requireActivity(), ChatFragment.class, crushArgs(c));
                    } else if (w == 1) {
                        Nav.push(requireActivity(), CrushDetailFragment.class, crushArgs(c));
                    } else if (w == 2) {
                        Nav.push(requireActivity(), CrushEditFragment.class, crushArgs(c));
                    } else if (w == 3) {
                        Ui.confirm(requireContext(), "删除", "确定删除「" + c.name + "」吗？", "删除", () ->
                                CrushApi.delete(c.id, new RestCallback<>(v -> {
                                    Ui.toast(requireContext(), "已删除 ♥", FriendlyToast.Type.SUCCESS);
                                    load();
                                }, e -> Ui.toast(requireContext(), e))));
                    }
                })
                .show();
    }

    static Bundle crushArgs(Crush c) {
        Bundle b = new Bundle();
        b.putString(EXTRA_CRUSH, GsonFactory.GSON.toJson(c));
        return b;
    }

    static Crush crushFrom(Bundle b) {
        if (b == null) return null;
        String json = b.getString(EXTRA_CRUSH);
        return json == null ? null : GsonFactory.GSON.fromJson(json, Crush.class);
    }
}