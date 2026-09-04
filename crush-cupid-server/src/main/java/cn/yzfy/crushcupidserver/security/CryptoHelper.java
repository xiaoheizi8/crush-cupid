package cn.yzfy.crushcupidserver.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 密码编码 / 验证码哈希工具（BCrypt + SHA-256）。
 * 密码使用 BCrypt cost=12 存储哈希；验证码仅存 SHA-256 哈希，明文不落库。
 */
@Component
public class CryptoHelper {

    private final BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder(12);

    /** BCrypt 编码密码 */
    public String encodePassword(String rawPassword) {
        return bCrypt.encode(rawPassword);
    }

    /** 校验密码是否匹配 */
    public boolean matchesPassword(String rawPassword, String encodedPassword) {
        return encodedPassword != null && bCrypt.matches(rawPassword, encodedPassword);
    }

    /** SHA-256 哈希（用于验证码），返回 64 位 hex */
    public String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String s = Integer.toHexString(0xff & b);
                if (s.length() == 1) {
                    hex.append('0');
                }
                hex.append(s);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
