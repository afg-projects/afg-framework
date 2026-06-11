package io.github.afgprojects.framework.ai.core.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 凭证加密服务，提供 API Key 的加密/解密/脱敏
 */
@Slf4j
@Service
public class CredentialService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKeySpec keySpec;

    public CredentialService(@Value("${afg.ai.credential.encryption-key:default-encryption-key-change-in-prod}") String encryptionKey) {
        var keyBytes = new byte[32];
        var srcBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(srcBytes, 0, keyBytes, 0, Math.min(srcBytes.length, 32));
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            var iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            var encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            var combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Failed to encrypt credential: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            var combined = Base64.getDecoder().decode(ciphertext);

            var iv = new byte[GCM_IV_LENGTH];
            var encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to decrypt credential: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public String mask(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }
}
