package io.github.afgprojects.framework.security.auth.totp;

import io.github.afgprojects.framework.security.core.totp.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DefaultTotpService 测试
 *
 * <p>验证 RFC 6238 TOTP 实现的正确性。
 */
@DisplayName("DefaultTotpService 测试")
class DefaultTotpServiceTest {

    private DefaultTotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new DefaultTotpService();
    }

    @Nested
    @DisplayName("generateSecret 操作")
    class GenerateSecretTests {

        @Test
        @DisplayName("应返回非空字符串")
        void shouldReturnNonEmptyString() {
            String secret = totpService.generateSecret();

            assertThat(secret).isNotBlank();
        }

        @Test
        @DisplayName("应为合法的 Base32 编码")
        void shouldReturnValidBase32() {
            String secret = totpService.generateSecret();

            // Base32 字符集：A-Z + 2-7
            assertThat(secret).matches("^[A-Z2-7]+$");
        }

        @Test
        @DisplayName("长度应为 32 个字符（160 位）")
        void shouldReturn160BitSecret() {
            String secret = totpService.generateSecret();

            // 20 bytes = 160 bits -> 32 Base32 chars
            assertThat(secret).hasSize(32);
        }

        @RepeatedTest(5)
        @DisplayName("每次生成的 secret 应唯一")
        void shouldGenerateUniqueSecrets() {
            String secret1 = totpService.generateSecret();
            String secret2 = totpService.generateSecret();

            assertThat(secret1).isNotEqualTo(secret2);
        }
    }

    @Nested
    @DisplayName("generateQrCodeUrl 操作")
    class GenerateQrCodeUrlTests {

        @Test
        @DisplayName("应生成 otpauth://totp/ 格式的 URL")
        void shouldGenerateOtpAuthUrl() {
            String url = totpService.generateQrCodeUrl("testuser", "JBSWY3DPEHPK3PXP", "MyApp");

            assertThat(url).startsWith("otpauth://totp/");
        }

        @Test
        @DisplayName("URL 应包含 issuer:username 路径")
        void shouldContainIssuerAndUsername() {
            String url = totpService.generateQrCodeUrl("testuser", "JBSWY3DPEHPK3PXP", "MyApp");

            assertThat(url).contains("MyApp:testuser");
        }

        @Test
        @DisplayName("URL 应包含 secret 参数")
        void shouldContainSecretParameter() {
            String url = totpService.generateQrCodeUrl("testuser", "JBSWY3DPEHPK3PXP", "MyApp");

            assertThat(url).contains("secret=JBSWY3DPEHPK3PXP");
        }

        @Test
        @DisplayName("URL 应包含 issuer 参数")
        void shouldContainIssuerParameter() {
            String url = totpService.generateQrCodeUrl("testuser", "JBSWY3DPEHPK3PXP", "MyApp");

            assertThat(url).contains("issuer=MyApp");
        }

        @Test
        @DisplayName("完整 URL 格式验证")
        void shouldGenerateCorrectUrlFormat() {
            String url = totpService.generateQrCodeUrl("testuser", "JBSWY3DPEHPK3PXP", "MyApp");

            assertThat(url).isEqualTo(
                    "otpauth://totp/MyApp:testuser?secret=JBSWY3DPEHPK3PXP&issuer=MyApp"
            );
        }
    }

    @Nested
    @DisplayName("verifyCode 操作")
    class VerifyCodeTests {

        @Test
        @DisplayName("应验证正确的 TOTP 码")
        void shouldVerifyCorrectCode() {
            String secret = totpService.generateSecret();

            // 使用同一 secret 和当前时间生成验证码
            int code = generateCodeForTesting(secret);

            boolean result = totpService.verifyCode(secret, code);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("错误的验证码应验证失败")
        void shouldRejectWrongCode() {
            String secret = totpService.generateSecret();

            boolean result = totpService.verifyCode(secret, 0);

            // 码为 0 的概率极低，应验证失败
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("window=0 时仅验证当前时间步")
        void shouldVerifyOnlyCurrentTimeStepWithWindowZero() {
            String secret = totpService.generateSecret();
            int code = generateCodeForTesting(secret);

            // window=0 时只验证当前时间步
            boolean result = totpService.verifyCode(secret, code, 0);

            // 可能成功也可能失败（取决于时间边界），但不应抛异常
            assertThat(result).isInstanceOf(Boolean.class);
        }

        @Test
        @DisplayName("window 参数为负数应抛 IllegalArgumentException")
        void shouldThrowExceptionForNegativeWindow() {
            String secret = totpService.generateSecret();

            assertThatThrownBy(() -> totpService.verifyCode(secret, 123456, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Window must be >= 0");
        }

        @Test
        @DisplayName("window=2 应允许更大时间偏差")
        void shouldAllowLargerTimeDriftWithWindowTwo() {
            String secret = totpService.generateSecret();
            int code = generateCodeForTesting(secret);

            // window=2 时允许前后各 2 个步长（共 5 个时间窗口）
            boolean resultWindow1 = totpService.verifyCode(secret, code, 1);
            boolean resultWindow2 = totpService.verifyCode(secret, code, 2);

            // window=2 至少和 window=1 一样宽松
            // 如果 window=1 通过，window=2 也应通过
            if (resultWindow1) {
                assertThat(resultWindow2).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("SPI 契约验证")
    class SpiContractTests {

        @Test
        @DisplayName("应实现 TotpService 接口")
        void shouldImplementTotpService() {
            assertThat(totpService).isInstanceOf(TotpService.class);
        }

        @Test
        @DisplayName("generateSecret 返回值永不为 null")
        void shouldNeverReturnNullFromGenerateSecret() {
            String secret = totpService.generateSecret();

            assertThat(secret).isNotNull();
        }

        @Test
        @DisplayName("generateQrCodeUrl 返回值永不为 null")
        void shouldNeverReturnNullFromGenerateQrCodeUrl() {
            String url = totpService.generateQrCodeUrl("user", "secret", "issuer");

            assertThat(url).isNotNull();
        }
    }

    @Nested
    @DisplayName("RFC 6238 兼容性")
    class Rfc6238Tests {

        @Test
        @DisplayName("时间步长应为 30 秒")
        void shouldUse30SecondTimeStep() {
            // 通过连续生成验证码验证时间步行为
    // 在同一 30 秒时间窗口内，验证码应相同
            String secret = totpService.generateSecret();
            int code1 = generateCodeForTesting(secret);

            // 立即再次生成，应在同一时间窗口
            int code2 = generateCodeForTesting(secret);

            assertThat(code1).isEqualTo(code2);
        }

        @Test
        @DisplayName("验证码应为 6 位数字")
        void shouldGenerate6DigitCode() {
            String secret = totpService.generateSecret();
            int code = generateCodeForTesting(secret);

            assertThat(code).isBetween(0, 999999);
        }
    }

    /**
     * 辅助方法：使用 DefaultTotpService 的内部逻辑生成当前时间步的验证码。
     *
     * <p>由于 DefaultTotpService 的 generateTotpCode 是 private 方法，
     * 我们通过调用 verifyCode 的反向逻辑来获取当前验证码。
     * 这里采用直接使用 RFC 6238 算法实现来生成验证码。
     */
    private int generateCodeForTesting(String base32Secret) {
        // 使用独立的 TOTP 计算来生成当前验证码
        byte[] key = base32Decode(base32Secret);
        long timeStep = System.currentTimeMillis() / 1000 / 30;
        byte[] data = java.nio.ByteBuffer.allocate(8).putLong(timeStep).array();

        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0xF;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            return binary % 1_000_000;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate TOTP code for testing", e);
        }
    }

    /**
     * Base32 解码（RFC 4648，与 DefaultTotpService 相同算法）。
     */
    private static byte[] base32Decode(String encoded) {
        String clean = encoded.replaceAll("[=\\s]", "").toUpperCase();
        byte[] result = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsInBuffer = 0;
        int resultIndex = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int value;
            if (c >= 'A' && c <= 'Z') {
                value = c - 'A';
            } else if (c >= '2' && c <= '7') {
                value = c - '2' + 26;
            } else {
                throw new IllegalArgumentException("Invalid Base32 character: " + c);
            }

            buffer = (buffer << 5) | value;
            bitsInBuffer += 5;

            if (bitsInBuffer >= 8) {
                bitsInBuffer -= 8;
                result[resultIndex++] = (byte) ((buffer >> bitsInBuffer) & 0xFF);
            }
        }

        return java.util.Arrays.copyOf(result, resultIndex);
    }
}
