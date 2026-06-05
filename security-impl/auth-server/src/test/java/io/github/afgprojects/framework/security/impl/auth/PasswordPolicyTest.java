package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.security.PasswordPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PasswordPolicy 测试
 */
@DisplayName("PasswordPolicy 测试")
class PasswordPolicyTest {

    @Nested
    @DisplayName("预定义策略")
    class PredefinedPolicyTests {

        @Test
        @DisplayName("DEFAULT 策略应有正确的配置")
        void shouldHaveCorrectDefaultConfig() {
            PasswordPolicy policy = PasswordPolicy.DEFAULT;

            assertThat(policy.minLength()).isEqualTo(8);
            assertThat(policy.requireUppercase()).isTrue();
            assertThat(policy.requireLowercase()).isTrue();
            assertThat(policy.requireDigit()).isTrue();
            assertThat(policy.requireSpecialChar()).isTrue();
        }

        @Test
        @DisplayName("LOOSE 策略应有正确的配置")
        void shouldHaveCorrectLooseConfig() {
            PasswordPolicy policy = PasswordPolicy.LOOSE;

            assertThat(policy.minLength()).isEqualTo(6);
            assertThat(policy.requireUppercase()).isFalse();
            assertThat(policy.requireLowercase()).isFalse();
            assertThat(policy.requireDigit()).isFalse();
            assertThat(policy.requireSpecialChar()).isFalse();
        }

        @Test
        @DisplayName("STRICT 策略应有正确的配置")
        void shouldHaveCorrectStrictConfig() {
            PasswordPolicy policy = PasswordPolicy.STRICT;

            assertThat(policy.minLength()).isEqualTo(12);
            assertThat(policy.requireUppercase()).isTrue();
            assertThat(policy.requireLowercase()).isTrue();
            assertThat(policy.requireDigit()).isTrue();
            assertThat(policy.requireSpecialChar()).isTrue();
        }
    }

    @Nested
    @DisplayName("Builder 模式")
    class BuilderTests {

        @Test
        @DisplayName("Builder 应能创建自定义策略")
        void shouldCreateCustomPolicy() {
            PasswordPolicy policy = PasswordPolicy.builder()
                    .minLength(10)
                    .requireUppercase(true)
                    .requireLowercase(true)
                    .requireDigit(false)
                    .requireSpecialChar(true)
                    .build();

            assertThat(policy.minLength()).isEqualTo(10);
            assertThat(policy.requireUppercase()).isTrue();
            assertThat(policy.requireLowercase()).isTrue();
            assertThat(policy.requireDigit()).isFalse();
            assertThat(policy.requireSpecialChar()).isTrue();
        }

        @Test
        @DisplayName("Builder 默认值应与 DEFAULT 一致")
        void shouldHaveSameDefaultsAsDefaultPolicy() {
            PasswordPolicy policy = PasswordPolicy.builder().build();

            assertThat(policy.minLength()).isEqualTo(PasswordPolicy.DEFAULT.minLength());
            assertThat(policy.requireUppercase()).isEqualTo(PasswordPolicy.DEFAULT.requireUppercase());
            assertThat(policy.requireLowercase()).isEqualTo(PasswordPolicy.DEFAULT.requireLowercase());
            assertThat(policy.requireDigit()).isEqualTo(PasswordPolicy.DEFAULT.requireDigit());
            assertThat(policy.requireSpecialChar()).isEqualTo(PasswordPolicy.DEFAULT.requireSpecialChar());
        }

        @Test
        @DisplayName("Builder 应支持链式调用")
        void shouldSupportChaining() {
            PasswordPolicy policy = PasswordPolicy.builder()
                    .minLength(16)
                    .requireUppercase(false)
                    .requireLowercase(false)
                    .requireDigit(false)
                    .requireSpecialChar(false)
                    .build();

            assertThat(policy.minLength()).isEqualTo(16);
        }
    }

    @Nested
    @DisplayName("Record 特性")
    class RecordTests {

        @Test
        @DisplayName("相同配置的策略应相等")
        void shouldBeEqualWhenSameConfig() {
            PasswordPolicy policy1 = new PasswordPolicy(8, true, true, true, true);
            PasswordPolicy policy2 = new PasswordPolicy(8, true, true, true, true);

            assertThat(policy1).isEqualTo(policy2);
            assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
        }

        @Test
        @DisplayName("不同配置的策略应不相等")
        void shouldNotBeEqualWhenDifferentConfig() {
            PasswordPolicy policy1 = new PasswordPolicy(8, true, true, true, true);
            PasswordPolicy policy2 = new PasswordPolicy(8, true, true, true, false);

            assertThat(policy1).isNotEqualTo(policy2);
        }

        @Test
        @DisplayName("toString 应包含所有字段")
        void shouldIncludeAllFieldsInToString() {
            PasswordPolicy policy = PasswordPolicy.DEFAULT;

            String str = policy.toString();

            assertThat(str).contains("minLength=8");
            assertThat(str).contains("requireUppercase=true");
            assertThat(str).contains("requireLowercase=true");
            assertThat(str).contains("requireDigit=true");
            assertThat(str).contains("requireSpecialChar=true");
        }
    }
}
