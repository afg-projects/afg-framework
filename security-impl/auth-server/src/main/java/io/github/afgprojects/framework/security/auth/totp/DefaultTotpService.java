package io.github.afgprojects.framework.security.auth.totp;

import io.github.afgprojects.framework.security.core.totp.TotpService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 默认 TOTP 服务实现。
 *
 * <p>基于 RFC 6238 标准实现 TOTP（基于时间的一次性密码）算法：
 * <ul>
 *   <li>使用 HmacSHA1 作为 MAC 算法</li>
 *   <li>Secret 使用 Base32 编码</li>
 *   <li>时间步长 30 秒</li>
 *   <li>验证码长度 6 位</li>
 *   <li>QR Code URL 使用 otpauth://totp/ 格式</li>
 * </ul>
 *
 * <p>纯 Java 实现，无外部依赖。
 *
 * @since 1.0.0
 */
@Slf4j
public class DefaultTotpService implements TotpService {

    private static final int SECRET_SIZE_BYTES = 20; // 160 bits
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int CODE_MODULUS = (int) Math.pow(10, CODE_DIGITS);
    private static final String HMAC_ALGORITHM = "HmacSHA1";

    /** Base32 编码字符表（RFC 4648）。 */
    private static final char[] BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    private final SecureRandom secureRandom;

    /**
     * 构造函数。
     */
    public DefaultTotpService() {
        this.secureRandom = new SecureRandom();
    }

    @Override
    @NonNull
    public String generateSecret() {
        byte[] bytes = new byte[SECRET_SIZE_BYTES];
        secureRandom.nextBytes(bytes);
        return base32Encode(bytes);
    }

    @Override
    @NonNull
    public String generateQrCodeUrl(@NonNull String username, @NonNull String secret, @NonNull String issuer) {
        // otpauth://totp/{issuer}:{username}?secret={secret}&issuer={issuer}
        return "otpauth://totp/" + issuer + ":" + username
                + "?secret=" + secret
                + "&issuer=" + issuer;
    }

    @Override
    public boolean verifyCode(@NonNull String secret, int code) {
        return verifyCode(secret, code, 1);
    }

    @Override
    public boolean verifyCode(@NonNull String secret, int code, int window) {
        if (window < 0) {
            throw new IllegalArgumentException("Window must be >= 0");
        }

        long currentTimeStep = getCurrentTimeStep();

        // 检查当前时间步及前后 window 个步长
        for (int i = -window; i <= window; i++) {
            long timeStep = currentTimeStep + i;
            int expectedCode = generateTotpCode(secret, timeStep);
            if (expectedCode == code) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成指定时间步的 TOTP 验证码。
     *
     * @param secret   Base32 编码的 Secret
     * @param timeStep 时间步
     * @return 6 位数字验证码
     */
    private int generateTotpCode(String secret, long timeStep) {
        byte[] key = base32Decode(secret);
        byte[] data = ByteBuffer.allocate(8).putLong(timeStep).array();

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data);

            // Dynamic truncation（RFC 4226）
            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % CODE_MODULUS;
        } catch (Exception e) {
            log.error("Failed to generate TOTP code", e);
            throw new RuntimeException("TOTP code generation failed", e);
        }
    }

    /**
     * 获取当前时间步。
     *
     * @return 当前时间步（Unix 时间 / 时间步长秒数）
     */
    private long getCurrentTimeStep() {
        return System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;
    }

    /**
     * Base32 编码（RFC 4648，无填充）。
     *
     * @param bytes 字节数组
     * @return Base32 编码字符串
     */
    private static String base32Encode(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        int buffer = 0;
        int bitsInBuffer = 0;

        for (byte b : bytes) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsInBuffer += 8;

            while (bitsInBuffer >= 5) {
                bitsInBuffer -= 5;
                int index = (buffer >> bitsInBuffer) & 0x1F;
                result.append(BASE32_CHARS[index]);
            }
        }

        if (bitsInBuffer > 0) {
            int index = (buffer << (5 - bitsInBuffer)) & 0x1F;
            result.append(BASE32_CHARS[index]);
        }

        return result.toString();
    }

    /**
     * Base32 解码（RFC 4648，忽略填充字符）。
     *
     * @param encoded Base32 编码字符串
     * @return 解码后的字节数组
     */
    private static byte[] base32Decode(String encoded) {
        // 移除填充字符
        String clean = encoded.replaceAll("[=\\s]", "").toUpperCase();

        byte[] result = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsInBuffer = 0;
        int resultIndex = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int value = base32CharToValue(c);
            if (value < 0) {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }

            buffer = (buffer << 5) | value;
            bitsInBuffer += 5;

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8;
                result[resultIndex++] = (byte) ((buffer >> bitsInBuffer) & 0xFF);
            }
        }

        return Arrays.copyOf(result, resultIndex);
    }

    /**
     * 将 Base32 字符转换为对应的数值。
     *
     * @param c Base32 字符
     * @return 对应数值（0-31），无效字符返回 -1
     */
    private static int base32CharToValue(char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= '2' && c <= '7') {
            return c - '2' + 26;
        }
        return -1;
    }
}
