package cn.yzfy.crushcupidserver.security;

import cn.yzfy.crushcupidserver.exception.BizException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * 邮箱验证码发送器（SMTP 版）。
 * <p>通过 {@link JavaMailSender} 走真实 SMTP 发送带有样式的中文 HTML 邮件；
 * 未配置真实 SMTP（spring.mail.host 为空）且 {@code crush.mail.mode=mock} 时，仅打印到日志（本地联调）。
 * <p>mode=smtp 但 SMTP 未配置/发送失败时，抛明确业务异常，让调用方/用户得知配置缺失。
 */
@Slf4j
@Component
public class EmailSender {

    private final JavaMailSender mailSender;

    private final String mode;
    private final String fromName;
    private final String from;
    private final String smtpUsername;

    /** 同一邮箱最短发送间隔（毫秒），内存兜底（主限流在 DB 层由 AuthLogic 做） */
    private static final long MIN_INTERVAL_MS = 60_000L;

    private volatile String lastEmail;
    private volatile long lastSentAt;

    public EmailSender(
            @Autowired(required = false) JavaMailSender mailSender,
            @Value("${crush.mail.mode:mock}") String mode,
            @Value("${crush.mail.from-name:CrushCupid}") String fromName,
            @Value("${crush.mail.from:}") String from,
            @Value("${spring.mail.username:}") String smtpUsername) {
        this.mailSender = mailSender;
        this.mode = mode == null ? "mock" : mode.trim().toLowerCase();
        this.fromName = fromName;
        this.from = from;
        this.smtpUsername = smtpUsername;
    }

    /**
     * 发送验证码。
     *
     * @param email   收件邮箱
     * @param code    6 位验证码
     * @param purpose 用途：REGISTER / RESET_PWD（决定标题与正文）
     */
    public void send(String email, String code, String purpose) {
        if ("smtp".equals(mode)) {
            sendReal(email, code, purpose);
        } else {
            log.info("[邮件验证码][MOCK][{}] 收件人={} 验证码={}", purpose, email, code);
        }
    }

    /** 频控：同一邮箱 60 秒内只允许发送 1 条（内存兜底；DB 层由 AuthLogic 做权威判定） */
    public boolean allowSend(String email) {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (email != null && email.equals(lastEmail)) {
                if (now - lastSentAt < MIN_INTERVAL_MS) {
                    return false;
                }
            }
            lastEmail = email;
            lastSentAt = now;
            return true;
        }
    }

    /** 校验失败时清内存频控记录（供测试/重试） */
    public void clear(String email) {
        synchronized (this) {
            if (email != null && email.equals(lastEmail)) {
                lastEmail = null;
                lastSentAt = 0;
            }
        }
    }

    // ---------------- 真实发送 ----------------

    private void sendReal(String email, String code, String purpose) {
        if (mailSender == null) {
            throw new BizException(500, "邮件未配置：spring.mail.host 未设置，请先在配置文件填写 SMTP 参数并启用 crush.mail.mode=smtp");
        }
        String sender = resolveSender();
        if (sender.isEmpty()) {
            throw new BizException(500, "邮件未配置发件人：spring.mail.username / crush.mail.from 为空");
        }
        boolean change = "RESET_PWD".equals(purpose);
        String subject = change ? "【CrushCupid】重置密码验证码" : "【CrushCupid】注册验证码";
        String html = buildHtml(change, code);
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
            helper.setFrom(sender, fromName);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(html, true); // true=HTML
            mailSender.send(mime);
            log.info("[邮件验证码][SMTP][{}] 已发送至 {}", purpose, email);
        } catch (Exception e) {
            log.error("邮件发送失败 email={} purpose={}", email, purpose, e);
            throw new BizException(500, "邮件发送失败，请稍后再试");
        }
    }

    /** 发件地址：优先 crush.mail.from，其次 spring.mail.username */
    private String resolveSender() {
        if (from != null && !from.isBlank()) {
            return from.trim();
        }
        return smtpUsername == null ? "" : smtpUsername.trim();
    }

    private String buildHtml(boolean change, String code) {
        String headline = change ? "重置你的密码" : "欢迎加入 CrushCupid";
        String tip = change
                ? "您正在申请重置账号密码，验证码如下（10 分钟内有效，请勿泄露）："
                : "您正在注册 CrushCupid 账号，验证码如下（10 分钟内有效，请勿泄露）：";
        return "<div style=\"font-family:'Microsoft YaHei',Arial,sans-serif;max-width:520px;margin:24px auto;"
                + "border:1px solid #eee;border-radius:10px;padding:28px;\">"
                + "<h2 style=\"color:#333;margin:0 0 12px;\">" + headline + "</h2>"
                + "<p style=\"color:#555;line-height:1.7;\">" + tip + "</p>"
                + "<div style=\"font-size:30px;font-weight:bold;letter-spacing:6px;color:#ff6b9d;"
                + "background:#fff5f8;border-radius:8px;text-align:center;padding:14px;margin:16px 0;\">"
                + code + "</div>"
                + "<p style=\"color:#999;font-size:12px;\">如果不是本人操作，请忽略本邮件。"
                + "验证码仅用于验证您的邮箱，CrushCupid 不会以任何形式索取验证码。</p>"
                + "</div>";
    }
}
