package cn.yzfy.crushApp.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import cn.yzfy.crushApp.R;

/** 通用 UI 工具：主线程分发、dp、胶囊标签、圆头像、卡片、加载框。 */
public final class Ui {
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return (int) ((v * c.getResources().getDisplayMetrics().density) + 0.5f);
    }

    public static void post(final Runnable r) {
        MAIN.post(r);
    }

    public static void toast(Context c, String s) {
        FriendlyToast.show(c, s);
    }

    public static void toast(Context c, String s, boolean longToast) {
        FriendlyToast.show(c, s, longToast);
    }

    public static void toast(Context c, String s, FriendlyToast.Type type) {
        FriendlyToast.show(c, s, type);
    }

    public static void toast(Context c, String s, FriendlyToast.Type type, boolean longToast) {
        FriendlyToast.show(c, s, type, longToast, null);
    }

    public static void toast(Context c, String s, FriendlyToast.Type type, boolean longToast, Runnable onTap) {
        FriendlyToast.show(c, s, type, longToast, onTap);
    }

    public static void confirm(Context c, String title, String message, String okText, Runnable onOk) {
        new MaterialAlertDialogBuilder(c)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(okText, (d, w) -> {
                    d.dismiss();
                    if (onOk != null) onOk.run();
                })
                .setNegativeButton("取消", (d, w) -> d.dismiss())
                .show();
    }

    /** 圆形头像（名字首字） */
    public static TextView avatar(Context c, String initial, @ColorInt int color, int sizeDp) {
        TextView tv = new TextView(c);
        int size = dp(c, sizeDp);
        tv.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        tv.setText(TextUtils.isEmpty(initial) ? "?" : initial.substring(0, 1));
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(sizeDp * 0.36f);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setBackground(circle(color));
        return tv;
    }

    public static GradientDrawable circle(@ColorInt int color) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setShape(GradientDrawable.OVAL);
        return d;
    }

    /** 圆角纯色块 */
    public static GradientDrawable rounded(@ColorInt int color, float radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusDp * 4); // px
        return d;
    }

    public static GradientDrawable rounded(float radiusDp) {
        return rounded(0, radiusDp);
    }

    public static GradientDrawable rounded(@ColorInt int color) {
        return rounded(color, 12);
    }

    /**
     * 密码输入框：横向容器内含输入框 + 「显示/隐藏」切换按钮（默认隐匿，点击显明文）。
     * 返回外层横向容器，传入的 EditText 会被包进容器。
     */
    public static LinearLayout passwordField(Context c, EditText et, String hint) {
        et.setHint(hint);
        et.setTextSize(15);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setSingleLine(true);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(dp(c, 14), dp(c, 10), dp(c, 4), dp(c, 10));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(et, new LinearLayout.LayoutParams(0,
                dp(c, 48), 1f));

        TextView eye = new TextView(c);
        eye.setText("显示");
        eye.setTextSize(12);
        eye.setTextColor(0xFFB07B8C);
        eye.setGravity(Gravity.CENTER);
        eye.setPadding(dp(c, 12), 0, dp(c, 12), 0);
        eye.setOnClickListener(v -> togglePassword(et, eye));
        row.addView(eye, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 48)));
        return row;
    }

    /** 密码可见性切换：默认隐匿，点击在密文/明文间切换，显明文时按钮高亮 */
    private static void togglePassword(EditText et, TextView eye) {
        boolean showing = (et.getInputType()
                & android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0;
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | (showing ? android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                        : android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD));
        et.setSelection(et.getText() == null ? 0 : et.getText().length());
        eye.setText(showing ? "显示" : "隐藏");
        eye.setTextColor(showing ? 0xFFB07B8C : 0xFFE8405F);
    }

    /** 胶囊标签 chip */
    public static TextView chip(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(0xFF9B5A66);
        tv.setPadding(dp(c, 8), dp(c, 3), dp(c, 8), dp(c, 3));
        tv.setBackground(ContextCompat.getDrawable(c, R.drawable.bg_chip));
        return tv;
    }

    /** 标签文字（secondary 描述） */
    public static TextView caption(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFFA5929C);
        return tv;
    }

    /** 小节标题 */
    public static TextView section(Context c, String text) {
        TextView tv = new TextView(c);
        tv.setText(text);
        tv.setTextSize(17);
        tv.setTextColor(0xFF2A2233);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setPadding(dp(c, 16), dp(c, 10), dp(c, 16), dp(c, 6));
        return tv;
    }

    /** 白色圆角卡片容器 */
    public static LinearLayout card(Context c, float radiusDp) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackground(rounded(0xFFFFFFFF, radiusDp));
        box.setElevation(dp(c, 2));
        box.setPadding(dp(c, 14), dp(c, 12), dp(c, 14), dp(c, 12));
        return box;
    }

    public static LinearLayout card(Context c) {
        return card(c, 18);
    }

    /** 垂直间距 */
    public static View space(Context c, float dpVal) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpVal)));
        return v;
    }

    public static Dialog loading(Context c, String message) {
        Dialog d = new Dialog(c);
        d.setCancelable(false);
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(c, 28), dp(c, 24), dp(c, 28), dp(c, 24));
        CircularProgressIndicator bar = new CircularProgressIndicator(c);
        bar.setIndeterminate(true);
        box.addView(bar, new ViewGroup.LayoutParams(dp(c, 56), dp(c, 56)));
        if (!TextUtils.isEmpty(message)) {
            TextView tv = new TextView(c);
            tv.setText(message);
            tv.setTextSize(14);
            tv.setTextColor(0xFF6B5E70);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(c, 12);
            box.addView(tv, lp);
        }
        d.setContentView(box);
        d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        d.show();
        return d;
    }

    public static void dismiss(Dialog d) {
        if (d != null && d.isShowing()) {
            try {
                Ui.post(d::dismiss);
            } catch (Exception ignored) {
            }
        }
    }

    public static void setBg(View v, android.graphics.drawable.Drawable d) {
        v.setBackground(d);
    }

    /** 列表首次可见时逐条淡入上滑（RecyclerView 布局动画） */
    public static void listEnter(androidx.recyclerview.widget.RecyclerView rv, int animRes) {
        rv.setLayoutAnimation(AnimationUtils.loadLayoutAnimation(rv.getContext(), animRes));
        rv.scheduleLayoutAnimation();
    }

    /** 下拉刷新容器：把目标 View 包进 SwipeRefreshLayout，返回外层以便 stopRefreshing */
    public static SwipeRefreshLayout pullRefresh(Context c, View child, Runnable onRefresh) {
        SwipeRefreshLayout srl = new SwipeRefreshLayout(c);
        srl.setColorSchemeColors(0xFFFF5A7A, 0xFF7256FF, 0xFFFFB35A);
        srl.setProgressBackgroundColorSchemeColor(0xFFFFFFFF);
        srl.setDistanceToTriggerSync(dp(c, 64));
        srl.setOnRefreshListener(onRefresh::run);
        srl.addView(child, new SwipeRefreshLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return srl;
    }

    /** 视图入场：淡入轻微上移 */
    public static void enter(View v, int animRes) {
        v.startAnimation(AnimationUtils.loadAnimation(v.getContext(), animRes));
    }

    /** 按钮按压反馈：按下缩小，抬起弹回 */
    public static void pressScale(View v) {
        v.setOnTouchListener((view, ev) -> {
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                    break;
            }
            return false;
        });
    }

    /** 给可点击行加前景涟漪（触摸反馈） */
    public static void ripple(View v) {
        int[] attrs = {android.R.attr.selectableItemBackground};
        android.content.res.TypedArray ta = v.getContext().obtainStyledAttributes(attrs);
        android.graphics.drawable.Drawable d = ta.getDrawable(0);
        ta.recycle();
        if (d != null) {
            v.setForeground(d);
        }
    }
}