package cn.yzfy.crushcupidserver.service.redis;

import cn.yzfy.crushcupidserver.model.entity.SysUserKey;
import cn.yzfy.crushcupidserver.security.AesGcmCrypto;
import cn.yzfy.crushcupidserver.service.SysUserKeyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;

/**
 * 用户聊天加密密钥提供者（点对点）。
 * <p>每个真实用户(user_id&gt;0)第一段聊天时惰性生成一个 32 字节随机 AES 密钥，
 * 用全局 KEK（{@link AesGcmCrypto}）包裹后落库 sys_user_key；随后解密时解包复用。
 * 同一用户多次并发首访通过唯一键冲突回读兜底，保证只生成一份。
 */
@Slf4j
@Component
public class UserKeyProvider {

    private static final SecureRandom RNG = new SecureRandom();

    private final AesGcmCrypto aesGcmCrypto;
    private final SysUserKeyService sysUserKeyService;

    public UserKeyProvider(AesGcmCrypto aesGcmCrypto, SysUserKeyService sysUserKeyService) {
        this.aesGcmCrypto = aesGcmCrypto;
        this.sysUserKeyService = sysUserKeyService;
    }

    /**
     * 取该用户的 32 字节 AES 密钥；不存在则生成并落库。
     *
     * @param userId 真实用户 id（须 &gt; 0）
     * @return 32 字节密钥；userId 非法返回 null（调用方走全局 KEK）
     */
    public byte[] userKey(long userId) {
        byte[] key = unwrap(sysUserKeyService.getByUserId(userId));
        if (key != null) {
            return key;
        }
        return createAndWrap(userId);
    }

    private byte[] unwrap(SysUserKey row) {
        if (row == null || row.getKeyEnc() == null || row.getKeyNonce() == null) {
            return null;
        }
        try {
            String b64 = aesGcmCrypto.decrypt(row.getKeyEnc(), row.getKeyNonce());
            if (b64 == null) {
                return null;
            }
            return Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            log.error("[UserKey] 用户 {} 密钥解包失败", row.getUserId(), e);
            return null;
        }
    }

    private byte[] createAndWrap(long userId) {
        byte[] key = new byte[32];
        RNG.nextBytes(key);
        String b64 = Base64.getEncoder().encodeToString(key);
        byte[] nonce = aesGcmCrypto.randomNonce();
        byte[] enc = aesGcmCrypto.encrypt(b64, nonce);

        SysUserKey row = new SysUserKey();
        row.setUserId(userId);
        row.setKeyEnc(enc);
        row.setKeyNonce(nonce);
        row.setCreatedAt(new Date());
        row.setUpdatedAt(new Date());
        try {
            sysUserKeyService.save(row);
            return key;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发首访：回读既有行
            return unwrap(sysUserKeyService.getByUserId(userId));
        }
    }
}
