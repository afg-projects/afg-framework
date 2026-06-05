package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginRequest 测试
 */
@DisplayName("LoginRequest 测试")
class LoginRequestTest {

    @Nested
    @DisplayName("LoginType 枚举")
    class LoginTypeTests {

        @Test
        @DisplayName("应包含所有登录类型")
        void shouldContainAllLoginTypes() {
            LoginRequest.LoginType[] types = LoginRequest.LoginType.values();

            assertThat(types).hasSize(4);
            assertThat(types).containsExactlyInAnyOrder(
                    LoginRequest.LoginType.USERNAME,
                    LoginRequest.LoginType.MOBILE,
                    LoginRequest.LoginType.EMAIL,
                    LoginRequest.LoginType.THIRD_PARTY
            );
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(LoginRequest.LoginType.valueOf("USERNAME")).isEqualTo(LoginRequest.LoginType.USERNAME);
            assertThat(LoginRequest.LoginType.valueOf("MOBILE")).isEqualTo(LoginRequest.LoginType.MOBILE);
            assertThat(LoginRequest.LoginType.valueOf("EMAIL")).isEqualTo(LoginRequest.LoginType.EMAIL);
            assertThat(LoginRequest.LoginType.valueOf("THIRD_PARTY")).isEqualTo(LoginRequest.LoginType.THIRD_PARTY);
        }
    }

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("ofUsername 应创建用户名密码登录请求")
        void shouldCreateUsernameLoginRequest() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password123");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.USERNAME);
            assertThat(request.username()).isEqualTo("admin");
            assertThat(request.password()).isEqualTo("password123");
            assertThat(request.mobile()).isNull();
            assertThat(request.email()).isNull();
        }

        @Test
        @DisplayName("ofMobile 应创建手机号登录请求")
        void shouldCreateMobileLoginRequest() {
            LoginRequest request = LoginRequest.ofMobile("13800138000", "123456");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.MOBILE);
            assertThat(request.mobile()).isEqualTo("13800138000");
            assertThat(request.captchaValue()).isEqualTo("123456");
            assertThat(request.username()).isNull();
            assertThat(request.password()).isNull();
        }

        @Test
        @DisplayName("ofEmail 应创建邮箱登录请求")
        void shouldCreateEmailLoginRequest() {
            LoginRequest request = LoginRequest.ofEmail("test@example.com", "123456");

            assertThat(request.loginType()).isEqualTo(LoginRequest.LoginType.EMAIL);
            assertThat(request.email()).isEqualTo("test@example.com");
            assertThat(request.captchaValue()).isEqualTo("123456");
            assertThat(request.username()).isNull();
            assertThat(request.password()).isNull();
        }
    }

    @Nested
    @DisplayName("record 特性")
    class RecordTests {

        @Test
        @DisplayName("应正确实现 equals 和 hashCode")
        void shouldImplementEqualsAndHashCode() {
            LoginRequest request1 = LoginRequest.ofUsername("admin", "password");
            LoginRequest request2 = LoginRequest.ofUsername("admin", "password");
            LoginRequest request3 = LoginRequest.ofUsername("admin", "different");

            assertThat(request1).isEqualTo(request2);
            assertThat(request1).hasSameHashCodeAs(request2);
            assertThat(request1).isNotEqualTo(request3);
        }

        @Test
        @DisplayName("应正确实现 toString")
        void shouldImplementToString() {
            LoginRequest request = LoginRequest.ofUsername("admin", "password");

            String str = request.toString();

            assertThat(str).contains("LoginRequest");
            assertThat(str).contains("USERNAME");
            assertThat(str).contains("admin");
        }
    }
}
