package io.github.afgprojects.framework.security.core.totp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpTotpService 测试
 */
@DisplayName("NoOpTotpService 测试")
class NoOpTotpServiceTest {

    private NoOpTotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new NoOpTotpService();
    }

    @Nested
    @DisplayName("generateSecret 操作")
    class GenerateSecretTests {

        @Test
        @DisplayName("应返回空字符串")
        void shouldReturnEmptyString() {
            String secret = totpService.generateSecret();

            assertThat(secret).isEmpty();
        }

        @Test
        @DisplayName("多次调用应始终返回空字符串")
        void shouldAlwaysReturnEmptyString() {
            String secret1 = totpService.generateSecret();
            String secret2 = totpService.generateSecret();

            assertThat(secret1).isEmpty();
            assertThat(secret2).isEmpty();
        }
    }

    @Nested
    @DisplayName("generateQrCodeUrl 操作")
    class GenerateQrCodeUrlTests {

        @Test
        @DisplayName("应返回空字符串")
        void shouldReturnEmptyString() {
            String url = totpService.generateQrCodeUrl("user", "secret", "issuer");

            assertThat(url).isEmpty();
        }

        @Test
        @DisplayName("参数不影响返回结果")
        void shouldIgnoreParameters() {
            String url1 = totpService.generateQrCodeUrl("user1", "secret1", "issuer1");
            String url2 = totpService.generateQrCodeUrl("user2", "secret2", "issuer2");

            assertThat(url1).isEmpty();
            assertThat(url2).isEmpty();
        }
    }

    @Nested
    @DisplayName("verifyCode 操作")
    class VerifyCodeTests {

        @Test
        @DisplayName("应返回 false")
        void shouldReturnFalse() {
            boolean result = totpService.verifyCode("any-secret", 123456);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("带 window 参数应返回 false")
        void shouldReturnFalseWithWindow() {
            boolean result = totpService.verifyCode("any-secret", 123456, 1);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("window=0 应返回 false")
        void shouldReturnFalseWithZeroWindow() {
            boolean result = totpService.verifyCode("any-secret", 123456, 0);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("window=2 应返回 false")
        void shouldReturnFalseWithLargeWindow() {
            boolean result = totpService.verifyCode("any-secret", 123456, 2);

            assertThat(result).isFalse();
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
}
