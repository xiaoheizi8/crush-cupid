package cn.yzfy.crushcupidserver.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.yzfy.crushcupidserver.model.vo.CaptchaVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形验证码（点击可刷新、登录前置防暴破）。
 * <p>实现：4 位字符 PNG 图片 + 干扰线/噪点；答案仅存 SHA-256 哈希于进程内内存，
 * 5 分钟过期、单次使用（校验后即删除，无论成败）。单实例部署足够；
 * 多实例场景可未来迁移到 Redis 存储。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    /** 验证码有效期（毫秒） */
    private static final long TTL_MS = 5 * 60 * 1000L;
    /** 字符集：去除易混淆的 0/O/1/I/L */
    private static final String CHARS = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    /** 验证码长度 */
    private static final int LENGTH = 4;
    /** 图片尺寸 */
    private static final int WIDTH = 132;
    private static final int HEIGHT = 48;

    private final cn.yzfy.crushcupidserver.security.CryptoHelper cryptoHelper;

    /** captchaId -> 答案哈希 + 过期时间 */
    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /** 生成验证码：返回 captchaId 与 data URI 图片 */
    public CaptchaVO generate() {
        purgeExpired();
        String code = RandomUtil.randomString(CHARS, LENGTH);
        String captchaId = RandomUtil.randomString(24);
        store.put(captchaId, new Entry(cryptoHelper.sha256(code), System.currentTimeMillis() + TTL_MS));
        CaptchaVO vo = new CaptchaVO();
        vo.setCaptchaId(captchaId);
        vo.setImage(renderPngDataUri(code));
        return vo;
    }

    /** 校验验证码（单次使用：无论成败均删除） */
    public boolean verify(String captchaId, String code) {
        if (StrUtil.isBlank(captchaId) || StrUtil.isBlank(code)) {
            return false;
        }
        Entry entry = store.remove(captchaId);
        if (entry == null || entry.expireAt < System.currentTimeMillis()) {
            return false;
        }
        return entry.hash.equals(cryptoHelper.sha256(code.trim().toUpperCase()));
    }

    private String renderPngDataUri(String code) {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 信纸底色（暖白），呼应登录页"一封写在深夜的信"
            g.setColor(new Color(251, 243, 228));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 干扰线条（火漆红 / 暗墨色，浅透明度）
            g.setStroke(new BasicStroke(1.2f));
            Color[] lineColors = {new Color(193, 56, 74, 60), new Color(74, 59, 78, 55), new Color(201, 178, 138, 50)};
            for (int i = 0; i < 5; i++) {
                g.setColor(lineColors[i % lineColors.length]);
                g.drawLine(RandomUtil.randomInt(WIDTH), RandomUtil.randomInt(HEIGHT),
                        RandomUtil.randomInt(WIDTH), RandomUtil.randomInt(HEIGHT));
            }
            // 噪点
            for (int i = 0; i < 45; i++) {
                g.setColor(new Color(RandomUtil.randomInt(90, 200), RandomUtil.randomInt(70, 170), RandomUtil.randomInt(90, 180)));
                g.fillRect(RandomUtil.randomInt(WIDTH), RandomUtil.randomInt(HEIGHT), 1, 1);
            }
            // 字符：逐字旋转 + 微偏移（墨色）
            g.setFont(new Font("Dialog", Font.BOLD, 26));
            int step = WIDTH / (LENGTH + 1);
            for (int i = 0; i < code.length(); i++) {
                double angle = (RandomUtil.randomDouble(-0.35, 0.35));
                int x = step * (i + 1) - 8 + RandomUtil.randomInt(-3, 4);
                int y = 34 + RandomUtil.randomInt(-4, 5);
                g.setColor(new Color(74, 59, 78));
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(code.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            log.error("验证码图片渲染失败: {}", e.getMessage());
            return "";
        }
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expireAt < now) {
                it.remove();
            }
        }
    }

    private record Entry(String hash, long expireAt) {
    }
}