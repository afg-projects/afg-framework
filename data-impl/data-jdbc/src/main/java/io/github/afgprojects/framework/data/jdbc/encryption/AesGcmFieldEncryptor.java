package io.github.afgprojects.framework.data.jdbc.encryption;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.encryption.BlindIndexProvider;
import io.github.afgprojects.framework.data.core.encryption.FieldEncryptionKeyProvider;
import io.github.afgprojects.framework.data.core.entity.FieldEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 生产级 AES-GCM 字段加密实现 + HMAC-SHA256 盲索引计算
 * <p>
 * 同时实现 {@link FieldEncryptor} 和 {@link BlindIndexProvider} 接口：
 * <ul>
 *   <li>加密：AES/GCM/NoPadding，随机 12 字节 IV，密文格式 = Base64(IV + ciphertext + GCM_TAG)</li>
 *   <li>盲索引：HMAC-SHA256，密钥与加密密钥分离，输出为十六进制字符串</li>
 * </ul>
 *
 * <h3>密文格式</h3>
 * <pre>
 * Base64(IV[12字节] + ciphertext[N字节] + GCM_TAG[16字节])
 * </pre>
 * <p>
 * IV 每次随机生成，相同明文产生不同密文（不可直接搜索，需通过盲索引查询）。
 *
 * <h3>盲索引</h3>
 * <pre>
 * Hex(HMAC-SHA256(plaintext, blindIndexKey))
 * </pre>
 * <p>
 * 相同明文产生相同盲索引值，支持等值查询。
 *
 * @see FieldEncryptor
 * @see BlindIndexProvider
 * @see FieldEncryptionKeyProvider
 */
@Slf4j
public class AesGcmFieldEncryptor implements FieldEncryptor, BlindIndexProvider {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final FieldEncryptionKeyProvider keyProvider;

    /**
     * 创建 AES-GCM 字段加密器
     *
     * @param keyProvider 密钥提供者
     */
    public AesGcmFieldEncryptor(FieldEncryptionKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    // ==================== FieldEncryptor ====================

    @Override
    public String encrypt(String plaintext, String algorithm, @Nullable String keyRef) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] key = keyProvider.getEncryptionKey(keyRef);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接: IV + ciphertext(with GCM tag)
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (InvalidKeyException e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "AES encryption key is invalid for keyRef '" + keyRef + "'", e);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                "AES-GCM encryption failed for keyRef '" + keyRef + "'", e);
        }
    }

    @Override
    public String decrypt(String ciphertext, String algorithm, @Nullable String keyRef) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            byte[] key = keyProvider.getEncryptionKey(keyRef);
            byte[] combined = Base64.getDecoder().decode(ciphertext);

            if (combined.length < GCM_IV_LENGTH + GCM_TAG_LENGTH / 8) {
                throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                    "Ciphertext too short for keyRef '" + keyRef + "'");
            }

            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);

            byte[] cipherPart = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherPart, 0, cipherPart.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(cipherPart);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (InvalidKeyException e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "AES decryption key is invalid for keyRef '" + keyRef + "'", e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                "AES-GCM decryption failed for keyRef '" + keyRef + "'", e);
        }
    }

    // ==================== BlindIndexProvider ====================

    @Override
    public String computeBlindIndex(String plaintext, String fieldName, @Nullable String keyRef) {
        if (plaintext == null || plaintext.isEmpty()) {
            return "";
        }
        try {
            byte[] blindIndexKey = keyProvider.getBlindIndexKey(keyRef);
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(blindIndexKey, HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (InvalidKeyException e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_KEY_INVALID,
                "HMAC blind index key is invalid for keyRef '" + keyRef + "'", e);
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.ENCRYPTION_ERROR,
                "Blind index computation failed for field '" + fieldName + "'", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
