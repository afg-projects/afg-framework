package io.github.afgprojects.framework.security.core.login;

import io.github.afgprojects.framework.security.core.login.model.CaptchaRequest;
import io.github.afgprojects.framework.security.core.login.model.CaptchaResponse;
import io.github.afgprojects.framework.security.core.login.model.LoginRequest;
import io.github.afgprojects.framework.security.core.login.model.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpLoginService 测试
 */
@DisplayName("NoOpLoginService 测试")
class NoOpLoginServiceTest {

    private NoOpLoginService loginService;

    @BeforeEach
    void setUp() {
        loginService = new NoOpLoginService();
    }

    @Nested
    @DisplayName("登录操作")
    class LoginTests {

        @Test
        @DisplayName("login 应返回空令牌的失败响应")
        void shouldReturnFailedLoginResponse() {
            LoginRequest request = LoginRequest.ofUsername("testuser", "password");

            LoginResponse response = loginService.login(request);

            assertThat(response.accessToken()).isNull();
            assertThat(response.userId()).isNull();
            assertThat(response.username()).isNull();
        }
    }

    @Nested
    @DisplayName("登出操作")
    class LogoutTests {

        @Test
        @DisplayName("logout 应不抛异常")
        void shouldNotThrowOnLogout() {
            loginService.logout("some-token");
        }
    }

    @Nested
    @DisplayName("令牌刷新操作")
    class RefreshTokenTests {

        @Test
        @DisplayName("refreshToken 应返回空令牌的失败响应")
        void shouldReturnFailedRefreshResponse() {
            LoginResponse response = loginService.refreshToken("some-refresh-token");

            assertThat(response.accessToken()).isNull();
            assertThat(response.userId()).isNull();
        }
    }

    @Nested
    @DisplayName("验证码操作")
    class CaptchaTests {

        @Test
        @DisplayName("generateCaptcha 应返回 noop key 的空响应")
        void shouldReturnNoopCaptchaResponse() {
            CaptchaRequest request = CaptchaRequest.ofImage();

            CaptchaResponse response = loginService.generateCaptcha(request);

            assertThat(response.captchaKey()).isEqualTo("noop");
        }

        @Test
        @DisplayName("validateCaptcha 应返回 false")
        void shouldReturnFalseOnValidate() {
            boolean result = loginService.validateCaptcha("any-key", "any-value");

            assertThat(result).isFalse();
        }
    }
}
