package io.github.afgprojects.framework.security.core.login.strategy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoginType 测试
 */
@DisplayName("LoginType 测试")
class LoginTypeTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含所有登录类型")
        void shouldContainAllLoginTypes() {
            LoginType[] types = LoginType.values();

            assertThat(types).hasSize(8);
            assertThat(types).containsExactlyInAnyOrder(
                    LoginType.USERNAME,
                    LoginType.MOBILE,
                    LoginType.EMAIL,
                    LoginType.THIRD_PARTY,
                    LoginType.WECHAT,
                    LoginType.DINGTALK,
                    LoginType.FEISHU,
                    LoginType.WECOM
            );
        }

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(LoginType.valueOf("USERNAME")).isEqualTo(LoginType.USERNAME);
            assertThat(LoginType.valueOf("MOBILE")).isEqualTo(LoginType.MOBILE);
            assertThat(LoginType.valueOf("EMAIL")).isEqualTo(LoginType.EMAIL);
            assertThat(LoginType.valueOf("THIRD_PARTY")).isEqualTo(LoginType.THIRD_PARTY);
            assertThat(LoginType.valueOf("WECHAT")).isEqualTo(LoginType.WECHAT);
            assertThat(LoginType.valueOf("DINGTALK")).isEqualTo(LoginType.DINGTALK);
            assertThat(LoginType.valueOf("FEISHU")).isEqualTo(LoginType.FEISHU);
            assertThat(LoginType.valueOf("WECOM")).isEqualTo(LoginType.WECOM);
        }
    }
}