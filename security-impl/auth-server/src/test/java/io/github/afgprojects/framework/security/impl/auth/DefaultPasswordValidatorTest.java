package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.security.DefaultPasswordValidator;
import io.github.afgprojects.framework.security.auth.security.PasswordPolicy;
import io.github.afgprojects.framework.security.core.security.PasswordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultPasswordValidator 测试
 */
@DisplayName("DefaultPasswordValidator 测试")
class DefaultPasswordValidatorTest {

    @Nested
    @DisplayName("默认策略验证")
    class DefaultPolicyTests {

        private final DefaultPasswordValidator validator = new DefaultPasswordValidator();

        @Test
        @DisplayName("符合所有要求的密码应验证通过")
        void shouldPassWhenPasswordMeetsAllRequirements() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1!");

            assertThat(result.valid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("密码太短应验证失败")
        void shouldFailWhenPasswordTooShort() {
            PasswordValidator.ValidationResult result = validator.validate("Ab1!");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("长度"));
        }

        @Test
        @DisplayName("缺少大写字母应验证失败")
        void shouldFailWhenMissingUppercase() {
            PasswordValidator.ValidationResult result = validator.validate("abcdef1!");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("大写"));
        }

        @Test
        @DisplayName("缺少小写字母应验证失败")
        void shouldFailWhenMissingLowercase() {
            PasswordValidator.ValidationResult result = validator.validate("ABCDEF1!");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("小写"));
        }

        @Test
        @DisplayName("缺少数字应验证失败")
        void shouldFailWhenMissingDigit() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdefg!");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("数字"));
        }

        @Test
        @DisplayName("缺少特殊字符应验证失败")
        void shouldFailWhenMissingSpecialChar() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef12");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("特殊字符"));
        }

        @Test
        @DisplayName("多个条件不满足应返回多个错误")
        void shouldReturnMultipleErrorsWhenMultipleConditionsFail() {
            PasswordValidator.ValidationResult result = validator.validate("abc");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).hasSizeGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("宽松策略验证")
    class LoosePolicyTests {

        private final DefaultPasswordValidator validator = new DefaultPasswordValidator(PasswordPolicy.LOOSE);

        @Test
        @DisplayName("6 位纯小写密码应验证通过")
        void shouldPassWithSixLowercaseChars() {
            PasswordValidator.ValidationResult result = validator.validate("abcdef");

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("5 位密码应验证失败")
        void shouldFailWithFiveChars() {
            PasswordValidator.ValidationResult result = validator.validate("abcde");

            assertThat(result.valid()).isFalse();
        }
    }

    @Nested
    @DisplayName("严格策略验证")
    class StrictPolicyTests {

        private final DefaultPasswordValidator validator = new DefaultPasswordValidator(PasswordPolicy.STRICT);

        @Test
        @DisplayName("12 位符合要求的密码应验证通过")
        void shouldPassWithTwelveCharPassword() {
            PasswordValidator.ValidationResult result = validator.validate("AbcdEfgh1!2@");

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("8 位符合要求的密码应验证失败（不够长）")
        void shouldFailWithEightCharPassword() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1!");

            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).anyMatch(e -> e.contains("长度"));
        }
    }

    @Nested
    @DisplayName("自定义策略验证")
    class CustomPolicyTests {

        @Test
        @DisplayName("只要求长度的策略应只检查长度")
        void shouldOnlyCheckLengthWhenOnlyLengthRequired() {
            PasswordPolicy policy = PasswordPolicy.builder()
                    .minLength(6)
                    .requireUppercase(false)
                    .requireLowercase(false)
                    .requireDigit(false)
                    .requireSpecialChar(false)
                    .build();
            DefaultPasswordValidator validator = new DefaultPasswordValidator(policy);

            PasswordValidator.ValidationResult result = validator.validate("abcdef");

            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("只要求特殊字符的策略应只检查特殊字符")
        void shouldOnlyCheckSpecialCharWhenOnlySpecialCharRequired() {
            PasswordPolicy policy = PasswordPolicy.builder()
                    .minLength(4)
                    .requireUppercase(false)
                    .requireLowercase(false)
                    .requireDigit(false)
                    .requireSpecialChar(true)
                    .build();
            DefaultPasswordValidator validator = new DefaultPasswordValidator(policy);

            PasswordValidator.ValidationResult result = validator.validate("a!b@");

            assertThat(result.valid()).isTrue();
        }
    }

    @Nested
    @DisplayName("密码编码和匹配")
    class EncodeAndMatchTests {

        private final DefaultPasswordValidator validator = new DefaultPasswordValidator();

        @Test
        @DisplayName("encode 应返回 BCrypt 加密后的密码")
        void shouldEncodePassword() {
            String encoded = validator.encode("mypassword");

            assertThat(encoded).isNotEqualTo("mypassword");
            assertThat(encoded).startsWith("$2a$");
        }

        @Test
        @DisplayName("matches 应正确匹配原始密码和加密密码")
        void shouldMatchRawAndEncodedPassword() {
            String rawPassword = "MyPassword1!";
            String encoded = validator.encode(rawPassword);

            assertThat(validator.matches(rawPassword, encoded)).isTrue();
        }

        @Test
        @DisplayName("matches 对错误密码应返回 false")
        void shouldNotMatchWrongPassword() {
            String encoded = validator.encode("MyPassword1!");

            assertThat(validator.matches("WrongPassword1!", encoded)).isFalse();
        }

        @Test
        @DisplayName("使用自定义 PasswordEncoder 构造应正常工作")
        void shouldWorkWithCustomPasswordEncoder() {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
            DefaultPasswordValidator validator = new DefaultPasswordValidator(PasswordPolicy.DEFAULT, encoder);

            String encoded = validator.encode("TestPass1!");
            assertThat(validator.matches("TestPass1!", encoded)).isTrue();
        }
    }

    @Nested
    @DisplayName("特殊字符范围验证")
    class SpecialCharTests {

        private final DefaultPasswordValidator validator = new DefaultPasswordValidator();

        @Test
        @DisplayName("感叹号应被识别为特殊字符")
        void shouldRecognizeExclamationMark() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1!");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("@ 符号应被识别为特殊字符")
        void shouldRecognizeAtSign() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1@");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("# 符号应被识别为特殊字符")
        void shouldRecognizeHash() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1#");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("$ 符号应被识别为特殊字符")
        void shouldRecognizeDollar() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1$");
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("下划线应被识别为特殊字符")
        void shouldRecognizeUnderscore() {
            PasswordValidator.ValidationResult result = validator.validate("Abcdef1_");
            assertThat(result.valid()).isTrue();
        }
    }
}
