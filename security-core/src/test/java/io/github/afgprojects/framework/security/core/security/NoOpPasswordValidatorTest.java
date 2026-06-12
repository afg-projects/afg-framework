package io.github.afgprojects.framework.security.core.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NoOpPasswordValidator 测试
 */
@DisplayName("NoOpPasswordValidator 测试")
class NoOpPasswordValidatorTest {

    private NoOpPasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NoOpPasswordValidator();
    }

    @Test
    @DisplayName("validate 应总是返回成功")
    void shouldAlwaysReturnSuccess() {
        PasswordValidator.ValidationResult result = validator.validate("weak");

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("matches 应总是返回 false")
    void shouldAlwaysReturnFalseOnMatches() {
        assertThat(validator.matches("password", "password")).isFalse();
    }

    @Test
    @DisplayName("encode 应原样返回密码")
    void shouldReturnPasswordAsIs() {
        assertThat(validator.encode("mypassword")).isEqualTo("mypassword");
    }
}
