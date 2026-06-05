package io.github.afgprojects.framework.security.core.security.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValidationResult 测试
 */
@DisplayName("ValidationResult 测试")
class ValidationResultTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("valid 应创建验证通过的结果")
        void shouldCreateValidResult() {
            ValidationResult result = ValidationResult.valid();

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMessage()).isNull();
            assertThat(result.getErrors()).isNull();
        }

        @Test
        @DisplayName("invalid 带消息应创建验证失败的结果")
        void shouldCreateInvalidResultWithMessage() {
            ValidationResult result = ValidationResult.invalid("Validation failed");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isEqualTo("Validation failed");
            assertThat(result.getErrors()).isNull();
        }

        @Test
        @DisplayName("invalid 带错误列表应创建验证失败的结果")
        void shouldCreateInvalidResultWithErrors() {
            List<String> errors = List.of("Error 1", "Error 2");
            ValidationResult result = ValidationResult.invalid(errors);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).isNull();
            assertThat(result.getErrors()).containsExactly("Error 1", "Error 2");
        }
    }

    @Nested
    @DisplayName("属性访问")
    class PropertyAccessTests {

        @Test
        @DisplayName("valid 结果的 isValid 应返回 true")
        void shouldReturnTrueForValidResult() {
            ValidationResult result = ValidationResult.valid();

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("invalid 结果的 isValid 应返回 false")
        void shouldReturnFalseForInvalidResult() {
            ValidationResult result = ValidationResult.invalid("Error");

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("valid 结果的 getErrors 应返回 null")
        void shouldReturnNullErrorsForValidResult() {
            ValidationResult result = ValidationResult.valid();

            assertThat(result.getErrors()).isNull();
        }

        @Test
        @DisplayName("带消息的 invalid 结果的 getErrors 应返回 null")
        void shouldReturnNullErrorsForInvalidResultWithMessage() {
            ValidationResult result = ValidationResult.invalid("Some error");

            assertThat(result.getErrors()).isNull();
        }

        @Test
        @DisplayName("带错误列表的 invalid 结果应正确获取 errors")
        void shouldReturnErrorsForInvalidResultWithErrors() {
            List<String> errors = List.of("Error 1", "Error 2");
            ValidationResult result = ValidationResult.invalid(errors);

            assertThat(result.getErrors()).hasSize(2);
            assertThat(result.getErrors()).containsExactly("Error 1", "Error 2");
        }

        @Test
        @DisplayName("invalid 带空错误列表应返回空列表")
        void shouldReturnEmptyListForEmptyErrors() {
            ValidationResult result = ValidationResult.invalid(List.of());

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).isEmpty();
        }
    }

}
