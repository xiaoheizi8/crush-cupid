package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.AuthApi;
import cn.yzfy.crushApp.api.ImageLoader;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.UserApi;
import cn.yzfy.crushApp.model.MyQuota;
import cn.yzfy.crushApp.model.User;

/** 用户中心：资料 / 配额 / 修改密码 / 退出登录 */
public class UserCenterFragment extends Fragment {

    private User me;
    private MyQuota quota;

    private TextView nameText;
    private TextView emailText;
    private TextView metaText;
    private TextView quotaText;

    /** 头像图片层（圆形裁剪）与无图时的首字母兜底层 */
    private ImageView avatarImage;
    private TextView avatarInitial;

    /** 系统相册选图，返回所选图片的 content Uri */
    private final ActivityResultLauncher<String> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onAvatarPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        android.content.Context ctx = requireContext();

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFBF3F5);
        root.addView(header(ctx, "用户中心"));

        ScrollView scroll = new ScrollView(ctx);
        scroll.setFillViewport(true);
        LinearLayout body = new LinearLayout(ctx);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 8), Ui.dp(ctx, 14), Ui.dp(ctx, 24));
        scroll.addView(body);

        // —— 资料卡 ——
        LinearLayout profile = Ui.card(ctx);
        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(buildAvatar(ctx));
        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.leftMargin = Ui.dp(ctx, 12);
        col.setLayoutParams(clp);
        nameText = new TextView(ctx);
        nameText.setTextSize(17);
        nameText.setTextColor(0xFF2A2233);
        nameText.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        col.addView(nameText);
        emailText = new TextView(ctx);
        emailText.setTextSize(12);
        emailText.setTextColor(0xFF6B5E70);
        col.addView(emailText);
        metaText = new TextView(ctx);
        metaText.setTextSize(11);
        metaText.setTextColor(0xFFA5929C);
        metaText.setPadding(0, Ui.dp(ctx, 2), 0, 0);
        col.addView(metaText);
        top.addView(col);
        TextView editBtn = linkChip(ctx, "改昵称");
        editBtn.setOnClickListener(v -> editUsername());
        top.addView(editBtn);
        profile.addView(top);
        body.addView(profile);

        // —— 配额卡 ——
        LinearLayout quotaCard = Ui.card(ctx);
        LinearLayout.LayoutParams qlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        qlp.topMargin = Ui.dp(ctx, 10);
        quotaCard.setLayoutParams(qlp);
        TextView quotaTitle = new TextView(ctx);
        quotaTitle.setText("用量与配额");
        quotaTitle.setTextSize(14);
        quotaTitle.setTextColor(0xFF2A2233);
        quotaTitle.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        quotaCard.addView(quotaTitle);
        quotaText = new TextView(ctx);
        quotaText.setTextSize(13);
        quotaText.setTextColor(0xFF6B5E70);
        quotaText.setLineSpacing(Ui.dp(ctx, 4), 1f);
        quotaText.setPadding(0, Ui.dp(ctx, 8), 0, 0);
        quotaCard.addView(quotaText);
        body.addView(quotaCard);

        // —— 操作列表 ——
        body.addView(Ui.space(ctx, 10));
        body.addView(actionRow(ctx, "🔑 修改密码", "验证旧密码后设置新密码", v -> changePassword()));
        body.addView(actionRow(ctx, "🎙 偏好音色", "到音色页试听 / 设计 ta 的声音", v ->
                Nav.push(requireActivity(), VoiceConfigFragment.class, null)));

        TextView tip = new TextView(ctx);
        tip.setText("⚠️ 本项目仅用于个人情感分析与回忆，\n不用于骚扰、跟踪或侵犯他人隐私。");
        tip.setTextSize(11);
        tip.setTextColor(0xFFC4AEB6);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(0, Ui.dp(ctx, 24), 0, 0);
        body.addView(tip);

        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(slp);
        root.addView(scroll);
        return root;
    }

    /** 圆形头像：ImageView（图片层，圆形裁剪）+ 首字母兜底层，点击唤起系统相册更换 */
    private View buildAvatar(android.content.Context ctx) {
        int size = Ui.dp(ctx, 52);
        FrameLayout box = new FrameLayout(ctx);
        box.setLayoutParams(new LinearLayout.LayoutParams(size, size));

        avatarImage = new ImageView(ctx);
        avatarImage.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        avatarImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarImage.setBackground(Ui.circle(0xFFFF5A7A));
        // API 21+：按背景 drawable（OVAL）的轮廓裁剪，使图片呈圆形
        avatarImage.setClipToOutline(true);
        avatarImage.setOutlineProvider(android.view.ViewOutlineProvider.BACKGROUND);
        box.addView(avatarImage);

        avatarInitial = Ui.avatar(ctx, "我", 0xFFFF5A7A, 52);
        avatarInitial.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        box.addView(avatarInitial);

        box.setOnClickListener(v -> pickAvatar.launch("image/*"));
        Ui.pressScale(box);
        return box;
    }

    /** 相册选中回调：读取字节 → 校验类型/大小 → 上传头像 → 刷新资料 */
    private void onAvatarPicked(Uri uri) {
        if (uri == null || !isAdded()) {
            return;
        }
        android.content.Context ctx = requireContext();
        final Dialog loading = Ui.loading(ctx, "上传中…");
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            if (in == null) {
                Ui.dismiss(loading);
                Ui.toast(this, "读取图片失败，请重试", FriendlyToast.Type.ERROR);
                return;
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            byte[] bytes = bos.toByteArray();
            if (bytes.length == 0) {
                Ui.dismiss(loading);
                Ui.toast(this, "图片内容为空", FriendlyToast.Type.WARN);
                return;
            }
            if (bytes.length > 5 * 1024 * 1024) {
                Ui.dismiss(loading);
                Ui.toast(this, "图片不能超过 5MB", FriendlyToast.Type.WARN);
                return;
            }
            String mime = ctx.getContentResolver().getType(uri);
            UserApi.uploadAvatar(bytes, "avatar", mime, new Rest.Callback<User>() {
                @Override
                public void ok(User data) {
                    Ui.dismiss(loading);
                    if (!isAdded()) {
                        return;
                    }
                    me = data;
                    render();
                    Ui.toast(UserCenterFragment.this, "头像已更新", FriendlyToast.Type.SUCCESS);
                }

                @Override
                public void fail(String message) {
                    Ui.dismiss(loading);
                    Ui.toast(UserCenterFragment.this, message, FriendlyToast.Type.ERROR, true);
                }
            });
        } catch (Exception e) {
            Ui.dismiss(loading);
            Ui.toast(this, "读取图片失败，请重试", FriendlyToast.Type.ERROR, true);
        }
    }

    private View header(android.content.Context ctx, String title) {
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
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(17);
        tv.setTextColor(0xFF2A2233);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        header.addView(tv, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return header;
    }

    private LinearLayout actionRow(android.content.Context ctx, String title, String sub, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackground(Ui.rounded(0xFFFFFFFF, 14));
        row.setElevation(Ui.dp(ctx, 1));
        row.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 10), Ui.dp(ctx, 14), Ui.dp(ctx, 10));
        Ui.ripple(row);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(ctx, 8);
        row.setLayoutParams(rlp);
        TextView t = new TextView(ctx);
        t.setText(title);
        t.setTextSize(14);
        t.setTextColor(0xFF2A2233);
        row.addView(t);
        TextView s = new TextView(ctx);
        s.setText(sub);
        s.setTextSize(11);
        s.setTextColor(0xFFA5929C);
        s.setPadding(0, Ui.dp(ctx, 2), 0, 0);
        row.addView(s);
        row.setOnClickListener(onClick);
        return row;
    }

    private TextView linkChip(android.content.Context ctx, String label) {
        TextView t = new TextView(ctx);
        t.setText(label);
        t.setTextSize(12);
        t.setTextColor(0xFFFF5A7A);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.rounded(0xFFFFF0F3, 999));
        t.setPadding(Ui.dp(ctx, 12), Ui.dp(ctx, 6), Ui.dp(ctx, 12), Ui.dp(ctx, 6));
        Ui.pressScale(t);
        return t;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        UserApi.me(new Rest.Callback<User>() {
            @Override
            public void ok(User data) {
                me = data;
                render();
            }

            @Override
            public void fail(String message) {
                Ui.toast(UserCenterFragment.this, message, FriendlyToast.Type.ERROR);
            }
        });
        UserApi.quota(new Rest.Callback<MyQuota>() {
            @Override
            public void ok(MyQuota data) {
                quota = data;
                render();
            }

            @Override
            public void fail(String message) {
                // 配额加载失败不阻塞资料展示
                quotaText.setText("配额加载失败：" + message);
            }
        });
    }

    private void render() {
        if (me != null) {
            nameText.setText(TextUtils.isEmpty(me.username) ? "未设置昵称" : me.username);
            emailText.setText(me.email == null ? "" : me.email);
            String verified = me.emailVerified ? "邮箱已验证" : "邮箱未验证";
            metaText.setText(verified + (me.createdAt == null || me.createdAt.isEmpty()
                    ? "" : " · 注册于 " + me.createdAt));
            // 头像：有 URL 时加载图片（圆形裁剪），否则隐藏图片层显示首字母兜底
            boolean hasAvatar = !TextUtils.isEmpty(me.avatarUrl);
            if (avatarImage != null) {
                if (hasAvatar) {
                    ImageLoader.load(avatarImage, me.avatarUrl);
                } else {
                    avatarImage.setImageDrawable(null);
                }
            }
            if (avatarInitial != null) {
                avatarInitial.setText(TextUtils.isEmpty(me.username) ? "我" : me.username.substring(0, 1));
                avatarInitial.setVisibility(hasAvatar ? View.GONE : View.VISIBLE);
            }
        }
        if (quota != null) {
            quotaText.setText("套餐：" + quota.planOr()
                    + "\n暗恋对象：" + quota.crushCountOr() + " / " + quota.crushLimitOr(5)
                    + "\n今日对话：" + quota.todayCountOr() + " / " + quota.dailyChatLimitOr(100) + " 条");
        }
    }

    /** 修改昵称 */
    private void editUsername() {
        android.content.Context ctx = requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 22), Ui.dp(ctx, 18), Ui.dp(ctx, 22), Ui.dp(ctx, 18));
        TextView head = new TextView(ctx);
        head.setText("修改昵称");
        head.setTextSize(17);
        head.setTextColor(0xFF2A2233);
        head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(head);

        final EditText input = new EditText(ctx);
        input.setTextSize(14);
        input.setText(me == null || me.username == null ? "" : me.username);
        input.setBackground(Ui.rounded(0xFFFBF6F7, 10));
        input.setPadding(Ui.dp(ctx, 10), Ui.dp(ctx, 8), Ui.dp(ctx, 10), Ui.dp(ctx, 8));
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ilp.topMargin = Ui.dp(ctx, 10);
        input.setLayoutParams(ilp);
        box.addView(input);

        LinearLayout btns = dialogButtons(ctx, d, "保存", () -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Ui.toast(ctx, "昵称不能为空", FriendlyToast.Type.WARN);
                return;
            }
            d.dismiss();
            Dialog loading = Ui.loading(ctx, "保存中…");
            UserApi.updateProfile(name, null, new Rest.Callback<User>() {
                @Override
                public void ok(User data) {
                    Ui.dismiss(loading);
                    me = data;
                    render();
                    Ui.toast(ctx, "昵称已更新", FriendlyToast.Type.SUCCESS);
                }

                @Override
                public void fail(String message) {
                    Ui.dismiss(loading);
                    Ui.toast(ctx, message, FriendlyToast.Type.ERROR, true);
                }
            });
        });
        box.addView(btns);
        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    /** 修改密码 */
    private void changePassword() {
        android.content.Context ctx = requireContext();
        Dialog d = new Dialog(ctx);
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(ctx, 22), Ui.dp(ctx, 18), Ui.dp(ctx, 22), Ui.dp(ctx, 18));
        TextView head = new TextView(ctx);
        head.setText("修改密码");
        head.setTextSize(17);
        head.setTextColor(0xFF2A2233);
        head.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        box.addView(head);

        final EditText oldPwd = new EditText(ctx);
        box.addView(pwdInput(ctx, oldPwd, "当前密码"));
        final EditText newPwd = new EditText(ctx);
        box.addView(pwdInput(ctx, newPwd, "新密码（8-64 位，含大小写字母和数字）"));
        final EditText newPwd2 = new EditText(ctx);
        box.addView(pwdInput(ctx, newPwd2, "确认新密码"));

        LinearLayout btns = dialogButtons(ctx, d, "确认修改", () -> {
            String oldP = oldPwd.getText().toString();
            String newP = newPwd.getText().toString();
            String newP2 = newPwd2.getText().toString();
            if (oldP.isEmpty() || newP.isEmpty()) {
                Ui.toast(ctx, "请填写完整", FriendlyToast.Type.WARN);
                return;
            }
            if (!newP.equals(newP2)) {
                Ui.toast(ctx, "两次输入的新密码不一致", FriendlyToast.Type.WARN);
                return;
            }
            d.dismiss();
            Dialog loading = Ui.loading(ctx, "提交中…");
            UserApi.changePassword(oldP, newP, new Rest.Callback<Void>() {
                @Override
                public void ok(Void data) {
                    Ui.dismiss(loading);
                    Ui.toast(ctx, "密码已修改，下次登录请使用新密码", FriendlyToast.Type.SUCCESS, true);
                }

                @Override
                public void fail(String message) {
                    Ui.dismiss(loading);
                    Ui.toast(ctx, message, FriendlyToast.Type.ERROR, true);
                }
            });
        });
        box.addView(btns);
        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
    }

    private View pwdInput(android.content.Context ctx, EditText et, String hint) {
        LinearLayout row = Ui.passwordField(ctx, et, hint);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(ctx, 10);
        row.setLayoutParams(lp);
        return row;
    }

    /** 对话框底部按钮组：取消 + 主操作 */
    private LinearLayout dialogButtons(android.content.Context ctx, Dialog d, String okText, Runnable onOk) {
        LinearLayout btns = new LinearLayout(ctx);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.END);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(ctx, 12);
        btns.setLayoutParams(blp);
        TextView cancel = new TextView(ctx);
        cancel.setText("取消");
        cancel.setTextSize(14);
        cancel.setTextColor(0xFFA5929C);
        cancel.setPadding(Ui.dp(ctx, 14), Ui.dp(ctx, 8), Ui.dp(ctx, 14), Ui.dp(ctx, 8));
        cancel.setOnClickListener(v -> d.dismiss());
        btns.addView(cancel);
        TextView ok = new TextView(ctx);
        ok.setText(okText);
        ok.setTextSize(14);
        ok.setTextColor(0xFFFFFFFF);
        ok.setBackground(Ui.rounded(0xFFFF5A7A, 12));
        ok.setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 8));
        ok.setOnClickListener(v -> onOk.run());
        btns.addView(ok);
        return btns;
    }
}
