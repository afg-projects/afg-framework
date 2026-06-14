package io.github.afgprojects.framework.security.auth.totp;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.security.core.totp.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TwoFactorAuthenticationService 测试
 *
 * <p>验证 2FA 生命周期：setup -> enable -> verify -> disable -> recovery。
 */
@DisplayName("TwoFactorAuthenticationService 测试")
class TwoFactorAuthenticationServiceTest {

    /**
     * 测试用 TOTP 服务，总是返回 true（验证码始终通过）。
     * 不使用 Mockito，而是实现真实的测试替身。
     */
    private static class AlwaysValidTotpService implements TotpService {

        @Override
        public String generateSecret() {
            return "JBSWY3DPEHPK3PXP";
        }

        @Override
        public String generateQrCodeUrl(String username, String secret, String issuer) {
            return "otpauth://totp/" + issuer + ":" + username + "?secret=" + secret + "&issuer=" + issuer;
        }

        @Override
        public boolean verifyCode(String secret, int code) {
            return true;
        }

        @Override
        public boolean verifyCode(String secret, int code, int window) {
            return true;
        }
    }

    /**
     * 测试用 TOTP 服务，总是返回 false（验证码始终失败）。
     */
    private static class AlwaysInvalidTotpService implements TotpService {

        @Override
        public String generateSecret() {
            return "JBSWY3DPEHPK3PXP";
        }

        @Override
        public String generateQrCodeUrl(String username, String secret, String issuer) {
            return "otpauth://totp/" + issuer + ":" + username + "?secret=" + secret + "&issuer=" + issuer;
        }

        @Override
        public boolean verifyCode(String secret, int code) {
            return false;
        }

        @Override
        public boolean verifyCode(String secret, int code, int window) {
            return false;
        }
    }

    /**
     * 可切换行为的 TOTP 服务：先允许 enable（返回 true），之后禁止（返回 false）。
     * 用于测试 disable2fa 时验证码错误的场景。
     */
    private static class ToggleableTotpService implements TotpService {

        private boolean acceptCodes = true;

        void rejectCodes() {
            acceptCodes = false;
        }

        @Override
        public String generateSecret() {
            return "JBSWY3DPEHPK3PXP";
        }

        @Override
        public String generateQrCodeUrl(String username, String secret, String issuer) {
            return "otpauth://totp/" + issuer + ":" + username + "?secret=" + secret + "&issuer=" + issuer;
        }

        @Override
        public boolean verifyCode(String secret, int code) {
            return acceptCodes;
        }

        @Override
        public boolean verifyCode(String secret, int code, int window) {
            return acceptCodes;
        }
    }

    private TwoFactorAuthenticationService service;
    private AlwaysValidTotpService validTotpService;

    @BeforeEach
    void setUp() {
        validTotpService = new AlwaysValidTotpService();
        service = new TwoFactorAuthenticationService(validTotpService);
    }

    @Nested
    @DisplayName("setup 操作")
    class SetupTests {

        @Test
        @DisplayName("应返回 TOTP 设置信息（secret + QR Code URL）")
        void shouldReturnTotpSetupResponse() {
            TwoFactorAuthenticationService.TotpSetupResponse response =
                    service.setup("user-1", "testuser", "MyApp");

            assertThat(response.secret()).isNotBlank();
            assertThat(response.qrCodeUrl()).isNotBlank();
        }

        @Test
        @DisplayName("QR Code URL 应包含用户名和发行者")
        void shouldContainUsernameAndIssuerInQrCodeUrl() {
            TwoFactorAuthenticationService.TotpSetupResponse response =
                    service.setup("user-1", "testuser", "MyApp");

            assertThat(response.qrCodeUrl()).contains("testuser");
            assertThat(response.qrCodeUrl()).contains("MyApp");
        }

        @Test
        @DisplayName("setup 后应存储用户的 TOTP Secret")
        void shouldStoreSecretAfterSetup() {
            service.setup("user-1", "testuser", "MyApp");

            String secret = service.getUserSecret("user-1");

            assertThat(secret).isNotBlank();
        }

        @Test
        @DisplayName("2FA 初始状态应为未启用")
        void shouldNotBeEnabledAfterSetup() {
            service.setup("user-1", "testuser", "MyApp");

            assertThat(service.is2faEnabled("user-1")).isFalse();
        }
    }

    @Nested
    @DisplayName("enable2fa 操作")
    class Enable2faTests {

        @Test
        @DisplayName("setup + 正确验证码后应能启用 2FA")
        void shouldEnable2faAfterSetupAndCorrectCode() {
            service.setup("user-1", "testuser", "MyApp");

            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            assertThat(service.is2faEnabled("user-1")).isTrue();
            assertThat(recoveryCodes).isNotEmpty();
        }

        @Test
        @DisplayName("启用后应返回 10 个恢复码")
        void shouldReturn10RecoveryCodes() {
            service.setup("user-1", "testuser", "MyApp");

            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            assertThat(recoveryCodes).hasSize(10);
        }

        @Test
        @DisplayName("每个恢复码应为 8 位字符")
        void shouldReturn8CharRecoveryCodes() {
            service.setup("user-1", "testuser", "MyApp");

            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            assertThat(recoveryCodes).allMatch(code -> code.length() == 8);
        }

        @Test
        @DisplayName("恢复码不应包含容易混淆的字符")
        void shouldExcludeAmbiguousCharacters() {
            service.setup("user-1", "testuser", "MyApp");

            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            // 排除 I, O, 0, 1 等容易混淆的字符
            String ambiguousChars = "IO01";
            for (String code : recoveryCodes) {
                for (char c : ambiguousChars.toCharArray()) {
                    assertThat(code).doesNotContain(String.valueOf(c));
                }
            }
        }

        @Test
        @DisplayName("未 setup 就 enable 应抛出 BusinessException")
        void shouldThrowWhenEnableWithoutSetup() {
            assertThatThrownBy(() -> service.enable2fa("user-1", 123456))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("验证码错误时应抛出 BusinessException")
        void shouldThrowWhenCodeIsWrong() {
            TotpService invalidTotpService = new AlwaysInvalidTotpService();
            TwoFactorAuthenticationService serviceWithInvalidCode =
                    new TwoFactorAuthenticationService(invalidTotpService);
            serviceWithInvalidCode.setup("user-1", "testuser", "MyApp");

            assertThatThrownBy(() -> serviceWithInvalidCode.enable2fa("user-1", 123456))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("disable2fa 操作")
    class Disable2faTests {

        @Test
        @DisplayName("启用后应能禁用 2FA")
        void shouldDisable2fa() {
            service.setup("user-1", "testuser", "MyApp");
            service.enable2fa("user-1", 123456);

            service.disable2fa("user-1", 123456);

            assertThat(service.is2faEnabled("user-1")).isFalse();
        }

        @Test
        @DisplayName("禁用后 Secret 应被清除")
        void shouldClearSecretAfterDisable() {
            service.setup("user-1", "testuser", "MyApp");
            service.enable2fa("user-1", 123456);

            service.disable2fa("user-1", 123456);

            assertThat(service.getUserSecret("user-1")).isNull();
        }

        @Test
        @DisplayName("未启用 2FA 时禁用应抛出 BusinessException")
        void shouldThrowWhenDisableWithoutEnable() {
            assertThatThrownBy(() -> service.disable2fa("user-1", 123456))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("禁用时验证码错误应抛出 BusinessException")
        void shouldThrowWhenDisableWithWrongCode() {
            ToggleableTotpService toggleableTotp = new ToggleableTotpService();
            TwoFactorAuthenticationService toggleService = new TwoFactorAuthenticationService(toggleableTotp);
            toggleService.setup("user-1", "testuser", "MyApp");
            toggleService.enable2fa("user-1", 123456);  // 此时 acceptCodes=true

            // 切换为拒绝验证码
            toggleableTotp.rejectCodes();

            assertThatThrownBy(() -> toggleService.disable2fa("user-1", 123456))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("verifyTotpCode 操作")
    class VerifyTotpCodeTests {

        @Test
        @DisplayName("setup 后应能验证 TOTP 码")
        void shouldVerifyTotpCodeAfterSetup() {
            service.setup("user-1", "testuser", "MyApp");

            boolean result = service.verifyTotpCode("user-1", 123456);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("未 setup 时验证应返回 false")
        void shouldReturnFalseWhenNoSecret() {
            boolean result = service.verifyTotpCode("user-1", 123456);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("恢复码操作")
    class RecoveryCodeTests {

        @Test
        @DisplayName("有效的恢复码应验证成功")
        void shouldVerifyValidRecoveryCode() {
            service.setup("user-1", "testuser", "MyApp");
            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            boolean result = service.verifyRecoveryCode("user-1", recoveryCodes.get(0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("恢复码只能使用一次")
        void shouldNotReuseRecoveryCode() {
            service.setup("user-1", "testuser", "MyApp");
            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            // 保存第一个恢复码（因为 verifyRecoveryCode 会修改原列表）
            String firstCode = recoveryCodes.get(0);

            // 第一次使用
            boolean first = service.verifyRecoveryCode("user-1", firstCode);
            assertThat(first).isTrue();

            // 第二次使用同一恢复码
            boolean second = service.verifyRecoveryCode("user-1", firstCode);
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("无效的恢复码应验证失败")
        void shouldRejectInvalidRecoveryCode() {
            service.setup("user-1", "testuser", "MyApp");
            service.enable2fa("user-1", 123456);

            boolean result = service.verifyRecoveryCode("user-1", "INVALID1");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("未启用 2FA 的用户验证恢复码应返回 false")
        void shouldReturnFalseForUserWithout2fa() {
            boolean result = service.verifyRecoveryCode("user-1", "ANYCODE1");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("使用一个恢复码后其他恢复码仍可用")
        void shouldKeepOtherRecoveryCodesAfterOneUse() {
            service.setup("user-1", "testuser", "MyApp");
            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            // 保存恢复码（因为 verifyRecoveryCode 会修改原列表）
            String firstCode = recoveryCodes.get(0);
            String secondCode = recoveryCodes.get(1);

            // 使用第一个
            service.verifyRecoveryCode("user-1", firstCode);

            // 第二个仍可用
            boolean result = service.verifyRecoveryCode("user-1", secondCode);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("所有恢复码用尽后应自动清除")
        void shouldClearRecoveryCodesWhenExhausted() {
            service.setup("user-1", "testuser", "MyApp");
            List<String> recoveryCodes = service.enable2fa("user-1", 123456);

            // 使用所有恢复码（需要复制列表，因为 verifyRecoveryCode 会修改原列表）
            List<String> codesCopy = new java.util.ArrayList<>(recoveryCodes);
            for (String code : codesCopy) {
                service.verifyRecoveryCode("user-1", code);
            }

            // 全部用尽后再验证应返回 false
            boolean result = service.verifyRecoveryCode("user-1", codesCopy.get(0));
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("is2faEnabled 操作")
    class Is2faEnabledTests {

        @Test
        @DisplayName("未 setup 的用户应返回 false")
        void shouldReturnFalseForUnknownUser() {
            assertThat(service.is2faEnabled("unknown-user")).isFalse();
        }

        @Test
        @DisplayName("setup 后但未 enable 应返回 false")
        void shouldReturnFalseAfterSetup() {
            service.setup("user-1", "testuser", "MyApp");

            assertThat(service.is2faEnabled("user-1")).isFalse();
        }

        @Test
        @DisplayName("enable 后应返回 true")
        void shouldReturnTrueAfterEnable() {
            service.setup("user-1", "testuser", "MyApp");
            service.enable2fa("user-1", 123456);

            assertThat(service.is2faEnabled("user-1")).isTrue();
        }
    }

    @Nested
    @DisplayName("getUserSecret 操作")
    class GetUserSecretTests {

        @Test
        @DisplayName("未 setup 的用户应返回 null")
        void shouldReturnNullForUnknownUser() {
            assertThat(service.getUserSecret("unknown-user")).isNull();
        }

        @Test
        @DisplayName("setup 后应返回存储的 Secret")
        void shouldReturnSecretAfterSetup() {
            service.setup("user-1", "testuser", "MyApp");

            assertThat(service.getUserSecret("user-1")).isNotBlank();
        }
    }
}
