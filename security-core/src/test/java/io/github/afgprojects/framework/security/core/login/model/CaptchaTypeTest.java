package io.github.afgprojects.framework.security.core.login.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CaptchaType 测试
 */
@DisplayName("CaptchaType 测试")
class CaptchaTypeTest {

    @Nested
    @DisplayName("枚举值")
    class EnumValueTests {

        @Test
        @DisplayName("应包含 IMAGE 类型")
        void shouldContainImageType() {
            assertThat(CaptchaType.IMAGE).isNotNull();
            assertThat(CaptchaType.IMAGE.name()).isEqualTo("IMAGE");
        }

        @Test
        @DisplayName("应包含 SMS 类型")
        void shouldContainSmsType() {
            assertThat(CaptchaType.SMS).isNotNull();
            assertThat(CaptchaType.SMS.name()).isEqualTo("SMS");
        }

        @Test
        @DisplayName("应包含 EMAIL 类型")
        void shouldContainEmailType() {
            assertThat(CaptchaType.EMAIL).isNotNull();
            assertThat(CaptchaType.EMAIL.name()).isEqualTo("EMAIL");
        }

        @Test
        @DisplayName("应包含所有三种类型")
        void shouldContainAllTypes() {
            CaptchaType[] types = CaptchaType.values();

            assertThat(types).hasSize(3);
            assertThat(types).containsExactlyInAnyOrder(
                    CaptchaType.IMAGE,
                    CaptchaType.SMS,
                    CaptchaType.EMAIL
            );
        }
    }

    @Nested
    @DisplayName("枚举操作")
    class EnumOperationTests {

        @Test
        @DisplayName("应能通过名称获取枚举值")
        void shouldGetValueByName() {
            assertThat(CaptchaType.valueOf("IMAGE")).isEqualTo(CaptchaType.IMAGE);
            assertThat(CaptchaType.valueOf("SMS")).isEqualTo(CaptchaType.SMS);
            assertThat(CaptchaType.valueOf("EMAIL")).isEqualTo(CaptchaType.EMAIL);
        }

        @Test
        @DisplayName("应能获取枚举序号")
        void shouldGetOrdinal() {
            assertThat(CaptchaType.IMAGE.ordinal()).isEqualTo(0);
            assertThat(CaptchaType.SMS.ordinal()).isEqualTo(1);
            assertThat(CaptchaType.EMAIL.ordinal()).isEqualTo(2);
        }
    }
}
