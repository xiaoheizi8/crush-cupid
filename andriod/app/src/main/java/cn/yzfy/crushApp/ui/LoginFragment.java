package cn.yzfy.crushApp.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.yzfy.crushApp.R;
import cn.yzfy.crushApp.api.AuthApi;
import cn.yzfy.crushApp.api.ImageLoader;
import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.model.CaptchaVO;
import cn.yzfy.crushApp.model.LoginVO;

/** 登录 / 注册：登录 = 邮箱 + 密码 + 图形验证码；注册 = 昵称 + 邮箱验证码 + 密码 */
public class LoginFragment extends Fragment {

    private boolean isLoginMode = true;

    private LinearLayout loginForm;
    private LinearLayout registerForm;

    private EditText loginEmail;
    private EditText loginPassword;
    private EditText loginCaptcha;
    private ImageView captchaImage;
    private String captchaId;

    private TextView loginSubmit;
    private TextView loginErr;

    private EditText regUsername;
    private EditText regEmail;
    private EditText regCode;
    private EditText regPassword;
    private TextView sendCodeBtn;
    private TextView registerSubmit;
    private TextView regErr;

    private CountDownTimer countdown;
    private boolean sendingCode;

    private TextView loginTab;
    private TextView regTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFFFBF3F5);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(Ui.dp(requireContext(), 24), Ui.dp(requireContext(), 48),
                Ui.dp(requireContext(), 24), Ui.dp(requireContext(), 40));
        scroll.addView(root);

        TextView title = new TextView(requireContext());
        title.setText("Cupid ♥ 恋爱模拟");
        title.setTextSize(26);
        title.setTextColor(0xFFE8405F);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(requireContext());
        subtitle.setText("登录 / 注册，开启你的心动旅程");
        subtitle.setTextSize(13);
        subtitle.setTextColor(0xFFB07B8C);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(requireContext(), 6);
        slp.bottomMargin = Ui.dp(requireContext(), 24);
        subtitle.setLayoutParams(slp);
        root.addView(subtitle);

        // Tab 切换
        LinearLayout tabs = new LinearLayout(requireContext());
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(requireContext(), 44));
        tabs.setLayoutParams(tlp);

        loginTab = tab("登录");
        regTab = tab("注册");
        tabs.addView(loginTab, tabLp());
        tabs.addView(regTab, tabLp());
        root.addView(tabs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        loginForm = buildLoginForm(root);
        registerForm = buildRegisterForm(root);

        refreshTabs();
        root.addView(loginForm);
        root.addView(registerForm);

        loadCaptcha();
        Ui.enter(loginForm, R.anim.fade_scale_in);
        return scroll;
    }

    private TextView tab(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(15);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setOnClickListener(v -> {
            isLoginMode = text.equals("登录");
            refreshTabs();
            if (isLoginMode) loadCaptcha();
        });
        return tv;
    }

    private LinearLayout.LayoutParams tabLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                Ui.dp(requireContext(), 44), 1f);
        return lp;
    }

    private void refreshTabs() {
        if (isLoginMode) {
            loginTab.setTextColor(0xFFE8405F);
            regTab.setTextColor(0xFFA5929C);
        } else {
            loginTab.setTextColor(0xFFA5929C);
            regTab.setTextColor(0xFFE8405F);
        }
        loginForm.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
        registerForm.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
        clearErr(loginErr);
        clearErr(regErr);
    }

    private LinearLayout buildLoginForm(LinearLayout root) {
        LinearLayout card = card(root);

        TextView emailLabel = label("邮箱");
        card.addView(emailLabel);
        loginEmail = input("your@email.com", InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        card.addView(loginEmail);

        card.addView(label("密码"));
        loginPassword = input("密码", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(loginPassword);

        card.addView(label("图形验证码"));
        LinearLayout captchaRow = new LinearLayout(requireContext());
        captchaRow.setOrientation(LinearLayout.HORIZONTAL);
        captchaRow.setGravity(Gravity.CENTER_VERTICAL);
        captchaRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        loginCaptcha = new EditText(requireContext());
        loginCaptcha.setHint("输入右侧字符");
        loginCaptcha.setTextSize(15);
        loginCaptcha.setInputType(InputType.TYPE_CLASS_TEXT);
        loginCaptcha.setSingleLine(true);
        loginCaptcha.setBackgroundResource(R.drawable.bg_input);
        loginCaptcha.setPadding(Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10),
                Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10));
        captchaRow.addView(loginCaptcha, new LinearLayout.LayoutParams(0,
                Ui.dp(requireContext(), 48), 1f));

        captchaImage = new ImageView(requireContext());
        captchaImage.setBackgroundResource(R.drawable.bg_input);
        captchaImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        captchaImage.setPadding(Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4),
                Ui.dp(requireContext(), 4), Ui.dp(requireContext(), 4));
        captchaImage.setOnClickListener(v -> loadCaptcha());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                Ui.dp(requireContext(), 108), Ui.dp(requireContext(), 48));
        clp.leftMargin = Ui.dp(requireContext(), 10);
        captchaImage.setLayoutParams(clp);
        captchaRow.addView(captchaImage);
        card.addView(captchaRow);

        TextView captchaTip = new TextView(requireContext());
        captchaTip.setText("看不清？点击图片刷新");
        captchaTip.setTextSize(11);
        captchaTip.setTextColor(0xFFA5929C);
        LinearLayout.LayoutParams ctlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ctlp.topMargin = Ui.dp(requireContext(), 4);
        captchaTip.setLayoutParams(ctlp);
        card.addView(captchaTip);

        loginErr = errorText();
        card.addView(loginErr);

        loginSubmit = submit("登 录");
        loginSubmit.setOnClickListener(v -> doLogin());
        Ui.pressScale(loginSubmit);
        card.addView(loginSubmit);

        return card;
    }

    private LinearLayout buildRegisterForm(LinearLayout root) {
        LinearLayout card = card(root);

        card.addView(label("昵称"));
        regUsername = input("昵称（可空）", InputType.TYPE_CLASS_TEXT);
        card.addView(regUsername);

        card.addView(label("邮箱"));
        LinearLayout emailRow = new LinearLayout(requireContext());
        emailRow.setOrientation(LinearLayout.HORIZONTAL);
        emailRow.setGravity(Gravity.CENTER_VERTICAL);

        regEmail = new EditText(requireContext());
        regEmail.setHint("your@email.com");
        regEmail.setTextSize(15);
        regEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        regEmail.setSingleLine(true);
        regEmail.setBackgroundResource(R.drawable.bg_input);
        regEmail.setPadding(Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10),
                Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10));
        emailRow.addView(regEmail, new LinearLayout.LayoutParams(0,
                Ui.dp(requireContext(), 48), 1f));

        sendCodeBtn = new TextView(requireContext());
        sendCodeBtn.setText("发送验证码");
        sendCodeBtn.setTextSize(13);
        sendCodeBtn.setTextColor(0xFF7256FF);
        sendCodeBtn.setGravity(Gravity.CENTER);
        sendCodeBtn.setBackground(Ui.rounded(0xFFEFEBFF, 999));
        sendCodeBtn.setPadding(Ui.dp(requireContext(), 12), 0, Ui.dp(requireContext(), 12), 0);
        sendCodeBtn.setOnClickListener(v -> sendRegisterCode());
        LinearLayout.LayoutParams sclp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, Ui.dp(requireContext(), 48));
        sclp.leftMargin = Ui.dp(requireContext(), 10);
        sendCodeBtn.setLayoutParams(sclp);
        emailRow.addView(sendCodeBtn);
        card.addView(emailRow);

        card.addView(label("邮箱验证码"));
        regCode = input("6 位验证码", InputType.TYPE_CLASS_NUMBER);
        card.addView(regCode);

        card.addView(label("密码"));
        regPassword = input("至少 8 位，含大小写字母和数字", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        card.addView(regPassword);

        regErr = errorText();
        card.addView(regErr);

        registerSubmit = submit("注 册");
        registerSubmit.setOnClickListener(v -> doRegister());
        Ui.pressScale(registerSubmit);
        card.addView(registerSubmit);

        return card;
    }

    private void loadCaptcha() {
        AuthApi.getCaptcha(new Rest.Callback<CaptchaVO>() {
            @Override
            public void ok(CaptchaVO data) {
                captchaId = data.captchaId;
                ImageLoader.load(captchaImage, data.image);
            }

            @Override
            public void fail(String message) {
                Ui.toast(requireContext(), "验证码加载失败：" + message);
            }
        });
    }

    private void sendRegisterCode() {
        String email = text(regEmail);
        if (email.isEmpty() || !email.contains("@")) {
            Ui.toast(requireContext(), "请先填写正确的邮箱");
            return;
        }
        if (sendingCode) return;
        sendingCode = true;
        sendCodeBtn.setEnabled(false);
        AuthApi.sendEmailCode(email, "REGISTER", new Rest.Callback<Void>() {
            @Override
            public void ok(Void data) {
                Ui.toast(requireContext(), "验证码已发送，请查收邮箱");
                startCountdown();
            }

            @Override
            public void fail(String message) {
                sendingCode = false;
                sendCodeBtn.setEnabled(true);
                Ui.toast(requireContext(), message, true);
            }
        });
    }

    private void startCountdown() {
        if (countdown != null) countdown.cancel();
        countdown = new CountDownTimer(60_000, 1_000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sendCodeBtn.setText((millisUntilFinished / 1_000) + "s 后重发");
            }

            @Override
            public void onFinish() {
                sendingCode = false;
                sendCodeBtn.setEnabled(true);
                sendCodeBtn.setText("发送验证码");
            }
        }.start();
    }

    private void doLogin() {
        String email = text(loginEmail);
        String password = text(loginPassword);
        String captcha = text(loginCaptcha);
        if (email.isEmpty() || password.isEmpty()) {
            setErr(loginErr, "请填写邮箱和密码");
            return;
        }
        if (captcha.isEmpty()) {
            setErr(loginErr, "请输入图形验证码");
            return;
        }
        loginSubmit.setEnabled(false);
        loginSubmit.setText("登录中…");
        clearErr(loginErr);
        AuthApi.login(email, password, captchaId == null ? "" : captchaId, captcha, new Rest.Callback<LoginVO>() {
            @Override
            public void ok(LoginVO data) {
                AuthApi.saveToken(data.tokenValue);
                Nav.reset(requireActivity(), new HomeFragment());
            }

            @Override
            public void fail(String message) {
                loginSubmit.setEnabled(true);
                loginSubmit.setText("登 录");
                setErr(loginErr, message);
                loadCaptcha();
            }
        });
    }

    private void doRegister() {
        String email = text(regEmail);
        String password = text(regPassword);
        String code = text(regCode);
        if (email.isEmpty() || password.isEmpty()) {
            setErr(regErr, "请填写邮箱和密码");
            return;
        }
        if (code.isEmpty()) {
            setErr(regErr, "请输入邮箱验证码");
            return;
        }
        registerSubmit.setEnabled(false);
        registerSubmit.setText("注册中…");
        clearErr(regErr);
        String username = text(regUsername);
        AuthApi.register(email, password, username, code, new Rest.Callback<LoginVO>() {
            @Override
            public void ok(LoginVO data) {
                AuthApi.saveToken(data.tokenValue);
                Nav.reset(requireActivity(), new HomeFragment());
            }

            @Override
            public void fail(String message) {
                registerSubmit.setEnabled(true);
                registerSubmit.setText("注 册");
                setErr(regErr, message);
            }
        });
    }

    // ---------------- 构建辅助 ----------------

    private LinearLayout card(LinearLayout root) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        int pad = Ui.dp(requireContext(), 22);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(requireContext(), 20);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView label(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(0xFF6B5E70);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(requireContext(), 14);
        lp.bottomMargin = Ui.dp(requireContext(), 4);
        tv.setLayoutParams(lp);
        return tv;
    }

    private EditText input(String hint, int inputType) {
        EditText et = new EditText(requireContext());
        et.setHint(hint);
        et.setTextSize(15);
        et.setInputType(inputType);
        et.setSingleLine(true);
        et.setBackgroundResource(R.drawable.bg_input);
        et.setPadding(Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10),
                Ui.dp(requireContext(), 14), Ui.dp(requireContext(), 10));
        return et;
    }

    private TextView errorText() {
        TextView tv = new TextView(requireContext());
        tv.setTextSize(12);
        tv.setTextColor(0xFFE8405F);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(requireContext(), 6);
        tv.setLayoutParams(lp);
        tv.setVisibility(View.GONE);
        return tv;
    }

    private TextView submit(String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(16);
        tv.setTextColor(0xFFFFFFFF);
        tv.setGravity(Gravity.CENTER);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setBackground(Ui.rounded(0xFFFF5A7A, 999));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(requireContext(), 48));
        lp.topMargin = Ui.dp(requireContext(), 22);
        tv.setLayoutParams(lp);
        return tv;
    }

    private String text(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void setErr(TextView tv, String msg) {
        tv.setText(msg);
        tv.setVisibility(View.VISIBLE);
    }

    private void clearErr(TextView tv) {
        tv.setText("");
        tv.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countdown != null) countdown.cancel();
    }
}