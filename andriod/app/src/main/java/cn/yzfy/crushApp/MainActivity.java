package cn.yzfy.crushApp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import cn.yzfy.crushApp.api.AppPrefs;
import cn.yzfy.crushApp.api.AuthApi;
import cn.yzfy.crushApp.api.Session;
import cn.yzfy.crushApp.ui.FriendlyToast;
import cn.yzfy.crushApp.ui.HomeFragment;
import cn.yzfy.crushApp.ui.LoginFragment;
import cn.yzfy.crushApp.ui.Nav;
import cn.yzfy.crushApp.ui.Ui;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppPrefs.init(this);
        // 登录态失效（token 过期/被挤下线）：清 token 并跳回登录页
        Session.setListener(() -> {
            Ui.toast(this, "登录已过期，请重新登录", FriendlyToast.Type.WARN, true);
            Nav.reset(this, new LoginFragment());
        });
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            int enter = R.anim.fade_scale_in;
            if (AuthApi.isLoggedIn()) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(enter, 0)
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(enter, 0)
                        .replace(R.id.fragment_container, new cn.yzfy.crushApp.ui.LoginFragment())
                        .commit();
            }
            // 立即执行替换，确保启动动画叠加在已显示的内容之上（否则 replace 会把 splash 一并清掉）
            getSupportFragmentManager().executePendingTransactions();
            showSplash();
        }
    }

    /** 启动过场动画：品牌心形弹出 + 标题字幕渐显，短暂停留后淡出进入主页/登录页。 */
    private void showSplash() {
        FrameLayout container = findViewById(R.id.fragment_container);

        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0xFFFFF8F9);
        overlay.setClickable(true);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);

        TextView heart = new TextView(this);
        heart.setText("♥");
        heart.setTextSize(72);
        heart.setTextColor(0xFFFF5A7A);
        heart.setGravity(Gravity.CENTER);
        heart.setTypeface(Typeface.DEFAULT_BOLD);
        col.addView(heart);

        TextView title = new TextView(this);
        title.setText("Cupid 恋爱模拟");
        title.setTextSize(26);
        title.setTextColor(0xFFE8405F);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 14);
        title.setLayoutParams(tlp);
        col.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("把心动写进剧本，让 AI 陪你奔赴每一段浪漫");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFFB07B8C);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 8);
        subtitle.setLayoutParams(slp);
        col.addView(subtitle);

        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clp.gravity = Gravity.CENTER;
        overlay.addView(col, clp);

        container.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 入场初始态
        heart.setScaleX(0.2f);
        heart.setScaleY(0.2f);
        heart.setAlpha(0f);
        title.setAlpha(0f);
        title.setTranslationY(Ui.dp(this, 20));
        subtitle.setAlpha(0f);
        subtitle.setTranslationY(Ui.dp(this, 16));

        // 心形弹出 + 文字渐显上移
        heart.animate().scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(520)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .start();
        title.animate().alpha(1f).translationY(0f).setStartDelay(160).setDuration(360).start();
        subtitle.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(360).start();

        // 心跳脉冲
        heart.animate().scaleX(1.08f).scaleY(1.08f).setStartDelay(700).setDuration(160)
                .withEndAction(() -> heart.animate().scaleX(1f).scaleY(1f).setDuration(180).start())
                .start();

        // 收场：整屏淡出后移除，露出下方内容
        overlay.postDelayed(() ->
                overlay.animate().alpha(0f).setDuration(420)
                        .withEndAction(() -> container.removeView(overlay))
                        .start(), 1500);
    }
}
