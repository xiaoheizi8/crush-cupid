package cn.yzfy.crushcupidserver.security;

import cn.yzfy.crushcupidserver.service.redis.UserKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 聊天记录点对点（按用户）加密器。
 * <p>会话记录按其「归属用户」（conversation.user_id，即正在对话的当前用户）决定加密密钥：
 * 真实用户(&gt;0)用该用户私有 AES 密钥，共享/未归属(user_id=0)用全局 KEK。
 * <p>同一信封格式 {@code base64Url(nonce).base64Url(cipher)}，nonce 固定 12 字节随机，
 * 兼容存量明文（非信封原样返回）。
 * <p>解密时先按归属用户密钥，失败再回退全局 KEK——兼容 Phase5 曾用全局密钥加密的历史行（滚动迁移）。
 */
@Slf4j
@Component
public class UserChatCipher {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;
    private static final int NONCE_B64_LEN = 16;
    private static final Pattern B64URL = Pattern.compile("[A-Za-z0-9_-]+");
    private static final SecureRandom RNG = new SecureRandom();

    /** 共享/未归属桶 */
    public static final long SHARED_BUCKET = 0L;

    private final AesGcmCrypto aesGcmCrypto;
    private final UserKeyProvider userKeyProvider;

    public UserChatCipher(AesGcmCrypto aesGcmCrypto, UserKeyProvider userKeyProvider) {
        this.aesGcmCrypto = aesGcmCrypto;
        this.userKeyProvider = userKeyProvider;
    }

    /** 按归属用户加密（写路径）。 */
    public String encryptForUser(String plain, Long ownerId) {
        return keyedEncrypt(resolveKey(ownerId), plain);
    }

    /** 按归属用户解密（读路径），兼容存量明文与历史全局密钥行。 */
    public String decryptForUser(String stored, Long ownerId) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        SecretKeySpec primary = resolveKey(ownerId);
        String plain = keyedDecrypt(primary, stored);
        if (plain != null) {
            return plain;
        }
        // 回退全局 KEK：兼容 Phase5 用全局密钥写过、或共享桶/换绑等历史行
        return keyedDecrypt(primary == global() ? primary : global(), stored);
    }

    /** 解析归属用户应使用的密钥：真实用户→私有密钥；否则→全局 KEK。 */
    private SecretKeySpec resolveKey(Long ownerId) {
        if (ownerId != null && ownerId > SHARED_BUCKET) {
            byte[] k = userKeyProvider.userKey(ownerId);
            if (k != null) {
                return new SecretKeySpec(k, "AES");
            }
        }
        return global();
    }

    private SecretKeySpec global() {
        return aesGcmCrypto.globalKeySpec();
    }

    // ============ AES-GCM 信封（统一 legacy 兼容） ============

    private String keyedEncrypt(SecretKeySpec key, String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            RNG.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] cipherBytes = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return b64u(nonce) + "." + b64u(cipherBytes);
        } catch (Exception e) {
            log.warn("[ChatCipher] 对话加密失败，回退明文存储", e);
            return plain;
        }
    }

    /** 解密信封；非信封/历史明文原样返回；解密失败返回 null。 */
    private String keyedDecrypt(SecretKeySpec key, String stored) {
        int dot = stored.indexOf('.');
        if (dot != NONCE_B64_LEN || !B64URL.matcher(stored.substring(0, dot)).matches()) {
            return stored;
        }
        try {
            byte[] nonce = b64ud(stored.substring(0, dot));
            if (nonce.length != NONCE_BYTES) {
                return stored;
            }
            byte[] cipher = b64ud(stored.substring(dot + 1));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return new String(c.doFinal(cipher), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static String b64u(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static byte[] b64ud(String s) {
        return Base64.getUrlDecoder().decode(s);
    }
}
