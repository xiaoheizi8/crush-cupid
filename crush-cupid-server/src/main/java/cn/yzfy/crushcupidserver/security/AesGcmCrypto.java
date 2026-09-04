package cn.yzfy.crushcupidserver.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 对称加解密组件。
 * 用于用户私有 LLM API Key 的落库加密：每条记录独立 12 字节随机 nonce，
 * 密钥（KEK）来自配置 {@code crush.security.keystore.secret}（base64 编码 32 字节），
 * 不进入代码库默认值仅用于开发环境，生产应从环境变量/KMS 注入。
 */
@Slf4j
@Component
public class AesGcmCrypto {

    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final String DEV_DEFAULT =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    public AesGcmCrypto(
            @Value("${crush.security.keystore.secret:}") String base64Secret,
            @Value("${spring.profiles.active:}") String activeProfile) {
        byte[] key = decodeKey(base64Secret, activeProfile);
        this.keySpec = new SecretKeySpec(key, "AES");
        log.info("AesGcmCrypto 初始化完成（AES-256-GCM，key 长度 {} bytes, profile={}）", key.length, activeProfile);
    }
    /** 解密；密文或 nonce 为空返回 null */
    public String decrypt(byte[] ciphertext, byte[] nonce) {
        if (ciphertext == null || nonce == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES-GCM 解密失败", e);
            return null;
        }
    }

    /** 加密并返回密文（调用方需持久化返回的密文，并自行生成/持有 nonce） */
    public byte[] encrypt(String plaintext, byte[] nonce) {
        if (plaintext == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, nonce));
            return cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /** 生成 12 字节随机 nonce */
    public byte[] randomNonce() {
        byte[] nonce = new byte[NONCE_BYTES];
        secureRandom.nextBytes(nonce);
        return nonce;
    }

    /** 暴露全局 KEK 的 SecretKeySpec，供按用户加密封装用户密钥 / 共享桶走全局密钥解密的场景使用 */
    public SecretKeySpec globalKeySpec() {
        return new SecretKeySpec(keySpec.getEncoded(), "AES");
    }

    private byte[] decodeKey(String base64Secret, String activeProfile) {
        boolean prod = activeProfile == null
                ? true
                : !activeProfile.toLowerCase().contains("dev");
        if (base64Secret == null || base64Secret.isBlank()) {
            // 未配置：生产直接拒绝，避免静默用缺省密钥加密生产数据
            if (prod) {
                throw new IllegalStateException(
                        "crush.security.keystore.secret 未配置：生产必须通过环境变量/KMS 注入 base64 编码的 32 字节密钥");
            }
            base64Secret = DEV_DEFAULT;
        }
        String trimmed = base64Secret.trim();
        byte[] key;
        try {
            key = Base64.getDecoder().decode(trimmed);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "crush.security.keystore.secret 不是合法的 base64，请检查配置", e);
        }
        if (key.length != 32) {
            throw new IllegalStateException(
                    "crush.security.keystore.secret 解码后长度应为 32 字节（AES-256），实际 " + key.length);
        }
        if (prod && trimmed.equals(DEV_DEFAULT)) {
            throw new IllegalStateException(
                    "crush.security.keystore.secret 仍为默认开发密钥，生产环境必须更换为独立密钥");
        }
        return key;
    }
}
