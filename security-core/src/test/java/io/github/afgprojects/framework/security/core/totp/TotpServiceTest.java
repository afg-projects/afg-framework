package io.github.afgprojects.framework.security.core.totp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TotpService SPI 接口契约测试
 *
 * <p>验证 TotpService 接口定义的契约约束，确保所有实现都遵循。
 */
@DisplayName("TotpService SPI 契约测试")
class TotpServiceTest {

    @Nested
    @DisplayName("接口契约")
    class InterfaceContractTests {

        @Test
        @DisplayName("NoOpTotpService 应满足 TotpService 契约")
        void noOpTotpServiceShouldSatisfyContract() {
            TotpService service = new NoOpTotpService();

            // generateSecret 永不为 null
            assertThat(service.generateSecret()).isNotNull();

            // generateQrCodeUrl 永不为 null
            assertThat(service.generateQrCodeUrl("user", "secret", "issuer")).isNotNull();

            // verifyCode 返回 boolean（不抛异常）
            assertThat(service.verifyCode("secret", 123456)).isFalse();
            assertThat(service.verifyCode("secret", 123456, 1)).isFalse();
        }
    }

    @Nested
    @DisplayName("NoOp 降级语义")
    class NoOpDegradationSemanticsTests {

        @Test
        @DisplayName("generateSecret 返回空值表示功能未启用")
        void shouldReturnEmptySecretWhenDisabled() {
            TotpService service = new NoOpTotpService();

            String secret = service.generateSecret();

            // NoOp 语义：空字符串表示 TOTP 功能未启用
            assertThat(secret).isEmpty();
        }

        @Test
        @DisplayName("verifyCode 返回 false 表示验证总是失败")
        void shouldAlwaysFailVerificationWhenDisabled() {
            TotpService service = new NoOpTotpService();

            // NoOp 语义：验证总是失败，因为 TOTP 功能未启用
            assertThat(service.verifyCode("any-secret", 123456)).isFalse();
            assertThat(service.verifyCode("any-secret", 0)).isFalse();
            assertThat(service.verifyCode("", 0)).isFalse();
        }
    }
}
