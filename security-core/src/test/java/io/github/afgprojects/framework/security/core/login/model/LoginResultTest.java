package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginResult 测试
 */
@DisplayName("LoginResult 测试")
class LoginResultTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含 SUCCESS 结果")
        void shouldContainSuccessResult() {
            assertThat(LoginResult.SUCCESS).isNotNull();
            assertThat(LoginResult.SUCCESS.name()).isEqualTo("SUCCESS");
        }

        @Test
        @DisplayName("应包含 FAILURE 结果")
        void shouldContainFailureResult() {
            assertThat(LoginResult.FAILURE).isNotNull();
            assertThat(LoginResult.FAILURE.name()).isEqualTo("FAILURE");
        }

        @Test
        @DisplayName("应包含 LOCKED 结果")
        void shouldContainLockedResult() {
            assertThat(LoginResult.LOCKED).isNotNull();
            assertThat(LoginResult.LOCKED.name()).isEqualTo("LOCKED");
        }

        @Test
        @DisplayName("应包含 CAPTCHA_ERROR 结果")
        void shouldContainCaptchaErrorResult() {
            assertThat(LoginResult.CAPTCHA_ERROR).isNotNull();
            assertThat(LoginResult.CAPTCHA_ERROR.name()).isEqualTo("CAPTCHA_ERROR");
        }

        @Test
        @DisplayName("应包含所有四种结果")
        void shouldContainAllResults() {
            LoginResult[] results = LoginResult.values();

            assertThat(results).hasSize(4);
            assertThat(results).containsExactlyInAnyOrder(
                    LoginResult.SUCCESS,
                    LoginResult.FAILURE,
                    LoginResult.LOCKED,
                    LoginResult.CAPTCHA_ERROR
            );
        }
    }

    @Nested
    @DisplayName("枚举操作")
    class EnumOperationTests {

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(LoginResult.valueOf("SUCCESS")).isEqualTo(LoginResult.SUCCESS);
            assertThat(LoginResult.valueOf("FAILURE")).isEqualTo(LoginResult.FAILURE);
            assertThat(LoginResult.valueOf("LOCKED")).isEqualTo(LoginResult.LOCKED);
            assertThat(LoginResult.valueOf("CAPTCHA_ERROR")).isEqualTo(LoginResult.CAPTCHA_ERROR);
        }

        @Test
        @DisplayName("应能获取枚举序号")
        void shouldGetOrdinal() {
            assertThat(LoginResult.SUCCESS.ordinal()).isEqualTo(0);
            assertThat(LoginResult.FAILURE.ordinal()).isEqualTo(1);
            assertThat(LoginResult.LOCKED.ordinal()).isEqualTo(2);
            assertThat(LoginResult.CAPTCHA_ERROR.ordinal()).isEqualTo(3);
        }
    }
}
