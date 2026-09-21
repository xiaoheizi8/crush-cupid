package cn.yzfy.crushApp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * 友好交互式提醒：底部圆角浮层，类型化图标配色，滑入上弹，点击消失或触发动作，
 * 自动倒计时消失，多提醒排队堆叠，尊重系统「减弱动画」。
 */
public final class FriendlyToast {

    public enum Type {
        INFO("♥", 0xFFFF5A7A),
        SUCCESS("✓", 0xFF2FBF71),
        WARN("!", 0xFFF5A623),
        ERROR("×", 0xFFFF4D6A);

        final String glyph;
        final int color;

        Type(String glyph, int color) {
            this.glyph = glyph;
            this.color = color;
        }
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Queue<ToastItem> QUEUE = new ArrayDeque<>();
    private static boolean showing;

    private static final long SHORT = 2200;
    private static final long LONG = 4000;
    private static final long IN = 280;
    private static final long OUT = 160;

    private FriendlyToast() {
    }

    private static final class ToastItem {
        final Activity activity;
        final String text;
        final Type type;
        final boolean longDuration;
        final Runnable action;

        ToastItem(Activity activity, String text, Type type, boolean longDuration, Runnable action) {
            this.activity = activity;
            this.text = text;
            this.type = type;
            this.longDuration = longDuration;
            this.action = action;
        }
    }

    public static void show(Context c, String text) {
        show(c, text, Type.INFO, false, null);
    }

    public static void show(Context c, String text, boolean longDuration) {
        show(c, text, Type.INFO, longDuration, null);
    }

    public static void show(Context c, String text, Type type) {
        show(c, text, type, false, null);
    }

    public static void show(Context c, String text, Type type, boolean longDuration, Runnable action) {
        if (c == null || TextUtils.isEmpty(text)) return;
        Activity activity = findActivity(c);
        if (activity == null) {
            Ui.post(() -> Toast.makeText(c, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
            return;
        }
        QUEUE.offer(new ToastItem(activity, text, type, longDuration, action));
        pump();
    }

    private static void pump() {
        if (showing) return;
        ToastItem item = QUEUE.poll();
        if (item == null) return;
        showing = true;
        internalShow(item);
    }

    private static void internalShow(ToastItem item) {
        Activity activity = item.activity;
        WindowManager wm = activity.getWindowManager();
        View toast = build(activity, item);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        lp.y = Ui.dp(activity, 18);
        try {
            wm.addView(toast, lp);
        } catch (Exception e) {
            Ui.post(() -> Toast.makeText(activity, item.text, Toast.LENGTH_SHORT).show());
            showing = false;
            pump();
            return;
        }

        boolean reduce = reduceMotion(activity);
        if (reduce) {
            toast.setAlpha(1f);
            toast.setTranslationY(0);
            toast.setScaleX(1f);
            toast.setScaleY(1f);
        } else {
            toast.setAlpha(0f);
            toast.setTranslationY(Ui.dp(activity, 40));
            toast.setScaleX(0.96f);
            toast.setScaleY(0.96f);
            toast.animate()
                    .alpha(1f)
                    .translationY(0)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(IN)
                    .setInterpolator(new DecelerateInterpolator(1.8f))
                    .start();
        }

        toast.setOnClickListener(v -> {
            Runnable action = item.action;
            dismiss(toast, item, activity);
            if (action != null) {
                try {
                    action.run();
                } catch (Exception ignored) {
                }
            }
        });
        Ui.pressScale(toast);

        MAIN.postDelayed(() -> dismiss(toast, item, activity),
                item.longDuration || item.type == Type.ERROR ? LONG : SHORT);
    }

    private static void dismiss(View toast, ToastItem item, Activity activity) {
        if (toast == null) return;
        boolean reduce = reduceMotion(activity);
        Runnable done = () -> {
            try {
                activity.getWindowManager().removeView(toast);
            } catch (Exception ignored) {
            }
            showing = false;
            pump();
        };
        if (reduce) {
            done.run();
            return;
        }
        toast.animate()
                .alpha(0f)
                .translationY(Ui.dp(activity, 20))
                .scaleX(0.97f)
                .scaleY(0.97f)
                .setDuration(OUT)
                .withEndAction(done)
                .start();
    }

    private static View build(Context c, ToastItem item) {
        FrameLayout host = new FrameLayout(c);

        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Ui.dp(c, 16), Ui.dp(c, 13), Ui.dp(c, 12), Ui.dp(c, 13));
        card.setBackground(Ui.rounded(0xFFFFFFFF, 22));
        card.setElevation(Ui.dp(c, 10));

        TextView icon = new TextView(c);
        icon.setText(item.type.glyph);
        icon.setTextSize(13);
        icon.setTextColor(0xFFFFFFFF);
        icon.setGravity(Gravity.CENTER);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        icon.setBackground(Ui.circle(item.type.color));
        int iconSize = Ui.dp(c, 30);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));

        TextView text = new TextView(c);
        text.setText(item.text);
        text.setTextSize(14);
        text.setTextColor(0xFF2A2233);
        text.setLineSpacing(Ui.dp(c, 3), 1f);
        text.setMaxLines(4);
        text.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = Ui.dp(c, 11);
        text.setLayoutParams(tlp);

        TextView close = new TextView(c);
        close.setText("✕");
        close.setTextSize(13);
        close.setTextColor(0xFFB8A8B0);
        close.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                Ui.dp(c, 28), Ui.dp(c, 28));
        clp.leftMargin = Ui.dp(c, 6);
        close.setLayoutParams(clp);

        card.addView(icon);
        card.addView(text);
        card.addView(close);

        FrameLayout.LayoutParams cardLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.leftMargin = Ui.dp(c, 16);
        cardLp.rightMargin = Ui.dp(c, 16);
        host.addView(card, cardLp);

        host.setClickable(true);
        return host;
    }

    private static Activity findActivity(Context c) {
        while (c != null) {
            if (c instanceof Activity) return (Activity) c;
            if (c instanceof ContextWrapper) {
                c = ((ContextWrapper) c).getBaseContext();
            } else {
                break;
            }
        }
        return null;
    }

    private static boolean reduceMotion(Context c) {
        try {
            return Settings.Global.getFloat(c.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f;
        } catch (Exception ignored) {
            return false;
        }
    }
}