package io.github.afgprojects.framework.core.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

@DisplayName("PhoneValidator 测试")
class PhoneValidatorTest extends BaseUnitTest {

    private PhoneValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new PhoneValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @ParameterizedTest
    @DisplayName("有效的手机号应该通过验证")
    @ValueSource(strings = {"13800138000", "15912345678", "18800001111", "17712345678"})
    void shouldReturnTrueWhenValidPhone(String phone) {
        assertThat(validator.isValid(phone, context)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("无效的手机号应该验证失败")
    @ValueSource(strings = {"12345678901", "1380013800", "138001380001", "abcdefghijk", "13800138"})
    void shouldReturnFalseWhenInvalidPhone(String phone) {
        assertThat(validator.isValid(phone, context)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("null 或空字符串应该通过验证（由 @NotNull/@NotBlank 处理）")
    @NullAndEmptySource
    void shouldReturnTrueWhenNullOrEmpty(String phone) {
        assertThat(validator.isValid(phone, context)).isTrue();
    }
}
