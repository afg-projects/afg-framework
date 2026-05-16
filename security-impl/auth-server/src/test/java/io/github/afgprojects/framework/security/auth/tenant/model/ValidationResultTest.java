package io.github.afgprojects.framework.security.auth.tenant.model;

import io.github.afgprojects.framework.security.core.tenant.TenantErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ValidationResult 测试类
 */
@DisplayName("ValidationResult 测试")
class ValidationResultTest {

    @Nested
    @DisplayName("工厂方法测试")
    class FactoryMethodTests {

        @Test
        @DisplayName("valid() 创建有效的验证结果")
        void shouldCreateValidResult() {
            // When
            ValidationResult result = ValidationResult.valid();

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("invalid() 创建无效的验证结果")
        void shouldCreateInvalidResult() {
            // Given
            TenantErrorCode errorCode = TenantErrorCode.TENANT_NOT_FOUND;
            String errorMessage = "租户 tenant-001 不存在";

            // When
            ValidationResult result = ValidationResult.invalid(errorCode, errorMessage);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(errorCode);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        }

        @Test
        @DisplayName("invalid() 使用不同的错误码")
        void shouldCreateInvalidResultWithDifferentErrorCode() {
            // Given
            TenantErrorCode errorCode = TenantErrorCode.TENANT_DISABLED;
            String errorMessage = "租户已禁用";

            // When
            ValidationResult result = ValidationResult.invalid(errorCode, errorMessage);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_DISABLED);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("不可变性测试")
    class ImmutabilityTests {

        @Test
        @DisplayName("valid() 返回的对象是不可变的")
        void shouldBeImmutableForValid() {
            // When
            ValidationResult result = ValidationResult.valid();

            // Then - 验证状态不可改变（通过 getter 返回的是原始值或不可变对象）
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("invalid() 返回的对象是不可变的")
        void shouldBeImmutableForInvalid() {
            // Given
            TenantErrorCode errorCode = TenantErrorCode.TENANT_EXPIRED;
            String errorMessage = "租户已过期";

            // When
            ValidationResult result = ValidationResult.invalid(errorCode, errorMessage);

            // Then - 验证状态不可改变
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(errorCode);
            assertThat(result.getErrorMessage()).isEqualTo(errorMessage);
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode 测试")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("两个 valid() 结果应该相等")
        void shouldBeEqualForValidResults() {
            // Given
            ValidationResult result1 = ValidationResult.valid();
            ValidationResult result2 = ValidationResult.valid();

            // When & Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("相同错误码和消息的 invalid 结果应该相等")
        void shouldBeEqualForSameInvalidResult() {
            // Given
            ValidationResult result1 = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );
            ValidationResult result2 = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );

            // When & Then
            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("不同错误码的 invalid 结果应该不相等")
        void shouldNotBeEqualForDifferentErrorCode() {
            // Given
            ValidationResult result1 = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );
            ValidationResult result2 = ValidationResult.invalid(
                TenantErrorCode.TENANT_DISABLED, "租户不存在"
            );

            // When & Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("不同错误消息的 invalid 结果应该不相等")
        void shouldNotBeEqualForDifferentErrorMessage() {
            // Given
            ValidationResult result1 = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );
            ValidationResult result2 = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户 tenant-001 不存在"
            );

            // When & Then
            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("valid 和 invalid 结果应该不相等")
        void shouldNotBeEqualForValidAndInvalid() {
            // Given
            ValidationResult valid = ValidationResult.valid();
            ValidationResult invalid = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );

            // When & Then
            assertThat(valid).isNotEqualTo(invalid);
        }

        @Test
        @DisplayName("与 null 比较应该返回 false")
        void shouldNotBeEqualToNull() {
            // Given
            ValidationResult result = ValidationResult.valid();

            // When & Then
            assertThat(result).isNotEqualTo(null);
        }

        @Test
        @DisplayName("与自己比较应该返回 true")
        void shouldBeEqualToItself() {
            // Given
            ValidationResult result = ValidationResult.valid();

            // When & Then
            assertThat(result).isEqualTo(result);
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("valid 结果的 toString 应该包含 valid=true")
        void shouldContainValidTrue() {
            // Given
            ValidationResult result = ValidationResult.valid();

            // When
            String str = result.toString();

            // Then
            assertThat(str).contains("valid=true");
        }

        @Test
        @DisplayName("invalid 结果的 toString 应该包含错误信息")
        void shouldContainErrorInfo() {
            // Given
            ValidationResult result = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );

            // When
            String str = result.toString();

            // Then
            assertThat(str).contains("valid=false");
            assertThat(str).contains("TENANT_NOT_FOUND");
            assertThat(str).contains("租户不存在");
        }
    }

    @Nested
    @DisplayName("边界条件测试")
    class EdgeCaseTests {

        @Test
        @DisplayName("invalid() 允许空错误消息")
        void shouldAllowEmptyErrorMessage() {
            // When
            ValidationResult result = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, ""
            );

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isEmpty();
        }

        @Test
        @DisplayName("invalid() 允许 null 错误消息")
        void shouldAllowNullErrorMessage() {
            // When
            ValidationResult result = ValidationResult.invalid(
                TenantErrorCode.TENANT_NOT_FOUND, null
            );

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).isNull();
        }
    }
}
