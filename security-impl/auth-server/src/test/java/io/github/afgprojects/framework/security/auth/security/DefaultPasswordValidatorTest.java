package io.github.afgprojects.framework.security.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.security.core.security.PasswordValidator;

/**
 * DefaultPasswordValidator 测试
 */
@DisplayName("DefaultPasswordValidator 测试")
class DefaultPasswordValidatorTest {

    private PasswordValidator passwordValidator;

    @BeforeEach
    void setUp() {
        // 使用默认配置：最小长度8，需要大小写、数字、特殊字符
        passwordValidator = new DefaultPasswordValidator();
    }

    @Nested
    @DisplayName("密码强度校验测试")
    class PasswordStrengthValidationTests {

        @Test
        @DisplayName("有效密码 - 满足所有要求")
        void shouldValidateValidPassword() {
            // given
            String password = "Password123!";

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("有效密码 - 包含多种特殊字符")
        void shouldValidatePasswordWithVariousSpecialChars() {
            // given
            String[] validPasswords = {
                    "Password123!",
                    "Password123@",
                    "Password123#",
                    "Password123$",
                    "Password123%",
                    "Password123^",
                    "Password123&",
                    "Password123*",
                    "Password123()",
                    "Password123_",
                    "Password123+",
                    "Password123-",
                    "Password123="
            };

            // when & then
            for (String password : validPasswords) {
                PasswordValidator.ValidationResult result = passwordValidator.validate(password);
                assertThat(result.valid())
                        .withFailMessage("Password '%s' should be valid, but got errors: %s", password, result.errors())
                        .isTrue();
            }
        }

        @Test
        @DisplayName("无效密码 - 太短")
        void shouldFailWhenPasswordTooShort() {
            // given
            String password = "Pass1!"; // 只有6个字符

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码长度不能少于8位");
        }

        @Test
        @DisplayName("无效密码 - 缺少大写字母")
        void shouldFailWhenMissingUppercase() {
            // given
            String password = "password123!"; // 没有大写字母

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码必须包含大写字母");
        }

        @Test
        @DisplayName("无效密码 - 缺少小写字母")
        void shouldFailWhenMissingLowercase() {
            // given
            String password = "PASSWORD123!"; // 没有小写字母

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码必须包含小写字母");
        }

        @Test
        @DisplayName("无效密码 - 缺少数字")
        void shouldFailWhenMissingDigit() {
            // given
            String password = "Password!"; // 没有数字

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码必须包含数字");
        }

        @Test
        @DisplayName("无效密码 - 缺少特殊字符")
        void shouldFailWhenMissingSpecialChar() {
            // given
            String password = "Password123"; // 没有特殊字符

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码必须包含特殊字符");
        }

        @Test
        @DisplayName("无效密码 - 多个错误")
        void shouldReportMultipleErrors() {
            // given
            String password = "pass"; // 太短、无大写、无数字、无特殊字符

            // when
            PasswordValidator.ValidationResult result = passwordValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).hasSize(4);
            assertThat(result.errors()).contains(
                    "密码长度不能少于8位",
                    "密码必须包含大写字母",
                    "密码必须包含数字",
                    "密码必须包含特殊字符");
        }
    }

    @Nested
    @DisplayName("密码匹配测试")
    class PasswordMatchTests {

        @Test
        @DisplayName("匹配正确的密码")
        void shouldMatchCorrectPassword() {
            // given
            String rawPassword = "Password123!";
            String encodedPassword = passwordValidator.encode(rawPassword);

            // when
            boolean matches = passwordValidator.matches(rawPassword, encodedPassword);

            // then
            assertThat(matches).isTrue();
        }

        @Test
        @DisplayName("不匹配错误的密码")
        void shouldNotMatchIncorrectPassword() {
            // given
            String rawPassword = "Password123!";
            String wrongPassword = "WrongPassword123!";
            String encodedPassword = passwordValidator.encode(rawPassword);

            // when
            boolean matches = passwordValidator.matches(wrongPassword, encodedPassword);

            // then
            assertThat(matches).isFalse();
        }

        @Test
        @DisplayName("空密码不匹配")
        void shouldNotMatchEmptyPassword() {
            // given
            String rawPassword = "Password123!";
            String encodedPassword = passwordValidator.encode(rawPassword);

            // when
            boolean matches = passwordValidator.matches("", encodedPassword);

            // then
            assertThat(matches).isFalse();
        }
    }

    @Nested
    @DisplayName("密码加密测试")
    class PasswordEncodeTests {

        @Test
        @DisplayName("加密密码返回BCrypt格式")
        void shouldEncodePasswordInBCryptFormat() {
            // given
            String password = "Password123!";

            // when
            String encoded = passwordValidator.encode(password);

            // then
            assertThat(encoded).isNotNull();
            assertThat(encoded).startsWith("$2a$"); // BCrypt 格式
            assertThat(encoded).hasSize(60); // BCrypt 固定长度
        }

        @Test
        @DisplayName("相同密码每次加密结果不同（加盐）")
        void shouldGenerateDifferentHashForSamePassword() {
            // given
            String password = "Password123!";

            // when
            String encoded1 = passwordValidator.encode(password);
            String encoded2 = passwordValidator.encode(password);

            // then
            assertThat(encoded1).isNotEqualTo(encoded2); // 不同的盐值
            assertThat(passwordValidator.matches(password, encoded1)).isTrue();
            assertThat(passwordValidator.matches(password, encoded2)).isTrue();
        }

        @Test
        @DisplayName("加密不同密码产生不同结果")
        void shouldGenerateDifferentHashForDifferentPasswords() {
            // given
            String password1 = "Password123!";
            String password2 = "Different123!";

            // when
            String encoded1 = passwordValidator.encode(password1);
            String encoded2 = passwordValidator.encode(password2);

            // then
            assertThat(encoded1).isNotEqualTo(encoded2);
        }
    }

    @Nested
    @DisplayName("自定义密码策略测试")
    class CustomPasswordPolicyTests {

        @Test
        @DisplayName("自定义最小长度")
        void shouldUseCustomMinLength() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(12, true, true, true, true));
            String password = "Pass1!"; // 6个字符，不满足12位

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isFalse();
            assertThat(result.errors()).contains("密码长度不能少于12位");
        }

        @Test
        @DisplayName("禁用特殊字符要求")
        void shouldAllowNoSpecialCharWhenDisabled() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(8, true, true, true, false));
            String password = "Password123"; // 无特殊字符

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("禁用数字要求")
        void shouldAllowNoDigitWhenDisabled() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(8, true, true, false, true));
            String password = "Password!"; // 无数字

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("禁用大写字母要求")
        void shouldAllowNoUppercaseWhenDisabled() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(8, false, true, true, true));
            String password = "password123!"; // 无大写字母

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("禁用小写字母要求")
        void shouldAllowNoLowercaseWhenDisabled() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(8, true, false, true, true));
            String password = "PASSWORD123!"; // 无小写字母

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
        }

        @Test
        @DisplayName("最宽松策略 - 只要求长度")
        void shouldValidateWithMinimalPolicy() {
            // given
            PasswordValidator customValidator = new DefaultPasswordValidator(
                    new PasswordPolicy(6, false, false, false, false));
            String password = "abcdef"; // 只有6个小写字母

            // when
            PasswordValidator.ValidationResult result = customValidator.validate(password);

            // then
            assertThat(result.valid()).isTrue();
        }
    }
}
