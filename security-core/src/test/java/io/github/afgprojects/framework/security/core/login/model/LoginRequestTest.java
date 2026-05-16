package io.github.afgprojects.framework.security.core.login.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Nested
    @DisplayName("用户名密码登录测试")
    class UsernameLoginTests {

        @Test
        @DisplayName("应创建用户名密码登录请求")
        void shouldCreateUsernameLoginRequest() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password123");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.USERNAME);
            assertThat(request.username()).isEqualTo("admin");
            assertThat(request.password()).isEqualTo("password123");
        }

        @Test
        @DisplayName("用户名密码登录时其他字段应为 null")
        void otherFieldsShouldBeNullForUsernameLogin() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password123");

            assertThat(request.mobile()).isNull();
            assertThat(request.email()).isNull();
            assertThat(request.captchaKey()).isNull();
            assertThat(request.captchaValue()).isNull();
            assertThat(request.tenantId()).isNull();
            assertThat(request.deviceId()).isNull();
            assertThat(request.deviceName()).isNull();
            assertThat(request.clientId()).isNull();
            assertThat(request.extra()).isNull();
        }
    }

    @Nested
    @DisplayName("手机号验证码登录测试")
    class MobileLoginTests {

        @Test
        @DisplayName("应创建手机号登录请求")
        void shouldCreateMobileLoginRequest() {
            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.MOBILE);
            assertThat(request.mobile()).isEqualTo("13800138000");
            assertThat(request.captchaValue()).isEqualTo("123456");
        }

        @Test
        @DisplayName("手机号登录时用户名密码应为 null")
        void usernameAndPasswordShouldBeNullForMobileLogin() {
            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(request.username()).isNull();
            assertThat(request.password()).isNull();
            assertThat(request.email()).isNull();
            assertThat(request.captchaKey()).isNull();
        }
    }

    @Nested
    @DisplayName("邮箱登录测试")
    class EmailLoginTests {

        @Test
        @DisplayName("应创建邮箱登录请求")
        void shouldCreateEmailLoginRequest() {
            LoginRequest request = LoginRequest.ofEmail("user@example.com", "654321");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.EMAIL);
            assertThat(request.email()).isEqualTo("user@example.com");
            assertThat(request.captchaValue()).isEqualTo("654321");
        }

        @Test
        @DisplayName("邮箱登录时其他字段应为 null")
        void otherFieldsShouldBeNullForEmailLogin() {
            LoginRequest request = LoginRequest.ofEmail("user@example.com", "654321");

            assertThat(request.username()).isNull();
            assertThat(request.password()).isNull();
            assertThat(request.mobile()).isNull();
            assertThat(request.captchaKey()).isNull();
        }
    }

    @Nested
    @DisplayName("扩展参数测试")
    class ExtraParamsTests {

        @Test
        @DisplayName("应设置租户 ID")
        void shouldSetTenantId() {
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    "admin",
                    "password",
                    null,
                    null,
                    null,
                    null,
                    "tenant-001",
                    null,
                    null,
                    null,
                    null,
                    null);

            assertThat(request.tenantId()).isEqualTo("tenant-001");
        }

        @Test
        @DisplayName("应设置设备信息")
        void shouldSetDeviceInfo() {
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    "admin",
                    "password",
                    null,
                    null,
                    null,
                    null,
                    null,
                    "device-123",
                    "iPhone 15",
                    "client-app",
                    null,
                    null);

            assertThat(request.deviceId()).isEqualTo("device-123");
            assertThat(request.deviceName()).isEqualTo("iPhone 15");
            assertThat(request.clientId()).isEqualTo("client-app");
        }

        @Test
        @DisplayName("应设置验证码信息")
        void shouldSetCaptchaInfo() {
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    "admin",
                    "password",
                    null,
                    null,
                    "captcha-key-001",
                    "abc123",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            assertThat(request.captchaKey()).isEqualTo("captcha-key-001");
            assertThat(request.captchaValue()).isEqualTo("abc123");
        }

        @Test
        @DisplayName("应设置扩展信息")
        void shouldSetExtraInfo() {
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    "admin",
                    "password",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "{\"rememberMe\":true}");

            assertThat(request.extra()).isEqualTo("{\"rememberMe\":true}");
        }
    }

    @Nested
    @DisplayName("登录类型枚举测试")
    class LoginTypeTests {

        @Test
        @DisplayName("应包含所有登录类型")
        void shouldContainAllLoginTypes() {
            LoginRequest.LoginType[] types = LoginRequest.LoginType.values();

            assertThat(types).hasSize(4);
            assertThat(types).contains(
                    LoginRequest.LoginType.USERNAME,
                    LoginRequest.LoginType.MOBILE,
                    LoginRequest.LoginType.EMAIL,
                    LoginRequest.LoginType.THIRD_PARTY);
        }
    }

    @Nested
    @DisplayName("null 值支持测试")
    class NullValueTests {

        @Test
        @DisplayName("应支持 null 用户名")
        void shouldSupportNullUsername() {
            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(request.username()).isNull();
        }

        @Test
        @DisplayName("应支持 null 密码")
        void shouldSupportNullPassword() {
            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(request.password()).isNull();
        }

        @Test
        @DisplayName("应支持 null 手机号")
        void shouldSupportNullMobile() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password");

            assertThat(request.mobile()).isNull();
        }

        @Test
        @DisplayName("应支持 null 邮箱")
        void shouldSupportNullEmail() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password");

            assertThat(request.email()).isNull();
        }

        @Test
        @DisplayName("应支持所有可选字段为 null")
        void shouldSupportAllOptionalFieldsNull() {
            LoginRequest request = new LoginRequest(
                    LoginRequest.LoginType.USERNAME,
                    "admin",
                    "password",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

            assertThat(request.mobile()).isNull();
            assertThat(request.email()).isNull();
            assertThat(request.captchaKey()).isNull();
            assertThat(request.captchaValue()).isNull();
            assertThat(request.tenantId()).isNull();
            assertThat(request.deviceId()).isNull();
            assertThat(request.deviceName()).isNull();
            assertThat(request.clientId()).isNull();
            assertThat(request.extra()).isNull();
        }
    }
}
