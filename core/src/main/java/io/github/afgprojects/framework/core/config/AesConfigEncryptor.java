package io.github.afgprojects.framework.core.config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AES-GCM 配置加密器
 * 使用 AES-256-GCM 算法进行加密
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class AesConfigEncryptor implements ConfigEncryptor {

    private static final Logger log = LoggerFactory.getLogger(AesConfigEncryptor.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    /**
     * 创建 AES 配置加密器
     *
     * @param secretKey 密钥（必须是 16、24 或 32 字节）
     */
    public AesConfigEncryptor(@NonNull String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Secret key must be 16, 24, or 32 bytes for AES-128, AES-192, or AES-256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    @Override
    @NonNull public String encrypt(@NonNull String plaintext) {
        try {
            // 生成随机 IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 组合 IV 和密文
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            // Base64 编码并添加前缀后缀
            return PREFIX + Base64.getEncoder().encodeToString(combined) + SUFFIX;
        } catch (GeneralSecurityException | RuntimeException e) {
            // 处理加解密异常和运行时异常（如 NullPointerException）
            log.error("Encryption failed", e);
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    @NonNull public String decrypt(@NonNull String ciphertext) {
        try {
            // 提取内容
            String content = extractContent(ciphertext);

            // Base64 解码
            byte[] combined = Base64.getDecoder().decode(content);

            // 验证长度：必须至少包含 IV（12字节）和一个 AES 块（16字节）的认证标签
            if (combined.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Ciphertext too short");
            }

            // 分离 IV 和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);

            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            // 解密
            byte[] plaintext = cipher.doFinal(encrypted);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // 处理加解密异常、Base64 解码错误或密文验证错误
            log.error("Decryption failed", e);
            throw new IllegalArgumentException("Decryption failed", e);
        }
    }

    @Override
    @NonNull public String prefix() {
        return PREFIX;
    }

    @Override
    @NonNull public String suffix() {
        return SUFFIX;
    }
}
