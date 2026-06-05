package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantValidationResult 测试
 */
@DisplayName("TenantValidationResult 测试")
class TenantValidationResultTest {

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethodTests {

        @Test
        @DisplayName("valid 应创建有效的验证结果")
        void shouldCreateValidResult() {
            TenantValidationResult result = TenantValidationResult.valid();

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("invalid 应创建无效的验证结果")
        void shouldCreateInvalidResult() {
            TenantValidationResult result = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_NOT_FOUND, "租户不存在: tenant-001"
            );

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_NOT_FOUND);
            assertThat(result.getErrorMessage()).isEqualTo("租户不存在: tenant-001");
        }

        @Test
        @DisplayName("invalid 带空参数应创建无效的验证结果")
        void shouldCreateInvalidResultWithNullParams() {
            TenantValidationResult result = TenantValidationResult.invalid(null, null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorCode()).isNull();
            assertThat(result.getErrorMessage()).isNull();
        }
    }

    @Nested
    @DisplayName("valid 结果单例")
    class ValidSingletonTests {

        @Test
        @DisplayName("valid 应返回同一实例")
        void shouldReturnSameInstanceForValid() {
            TenantValidationResult result1 = TenantValidationResult.valid();
            TenantValidationResult result2 = TenantValidationResult.valid();

            assertThat(result1).isSameAs(result2);
        }
    }

    @Nested
    @DisplayName("equals 和 hashCode")
    class EqualsHashCodeTests {

        @Test
        @DisplayName("相同属性的 valid 结果应相等")
        void shouldBeEqualForValidResults() {
            TenantValidationResult result1 = TenantValidationResult.valid();
            TenantValidationResult result2 = TenantValidationResult.valid();

            assertThat(result1).isEqualTo(result2);
            assertThat(result1).hasSameHashCodeAs(result2);
        }

        @Test
        @DisplayName("相同属性的 invalid 结果应相等")
        void shouldBeEqualForInvalidResults() {
            TenantValidationResult result1 = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_DISABLED, "租户已禁用"
            );
            TenantValidationResult result2 = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_DISABLED, "租户已禁用"
            );

            assertThat(result1).isEqualTo(result2);
            assertThat(result1).hasSameHashCodeAs(result2);
        }

        @Test
        @DisplayName("不同属性的 invalid 结果应不相等")
        void shouldNotBeEqualForDifferentInvalidResults() {
            TenantValidationResult result1 = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_DISABLED, "租户已禁用"
            );
            TenantValidationResult result2 = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_NOT_FOUND, "租户不存在"
            );

            assertThat(result1).isNotEqualTo(result2);
        }

        @Test
        @DisplayName("valid 和 invalid 结果应不相等")
        void shouldNotBeEqualForValidAndInvalid() {
            TenantValidationResult valid = TenantValidationResult.valid();
            TenantValidationResult invalid = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_DISABLED, "租户已禁用"
            );

            assertThat(valid).isNotEqualTo(invalid);
        }
    }

    @Nested
    @DisplayName("toString")
    class ToStringTests {

        @Test
        @DisplayName("valid 结果的 toString 应包含 valid=true")
        void shouldContainValidTrueForValidResult() {
            TenantValidationResult result = TenantValidationResult.valid();

            assertThat(result.toString()).contains("valid=true");
        }

        @Test
        @DisplayName("invalid 结果的 toString 应包含 valid=false 和错误信息")
        void shouldContainValidFalseAndErrorInfoForInvalidResult() {
            TenantValidationResult result = TenantValidationResult.invalid(
                    TenantErrorCode.TENANT_DISABLED, "租户已禁用"
            );

            String str = result.toString();
            assertThat(str).contains("valid=false");
            assertThat(str).contains("TENANT_DISABLED");
            assertThat(str).contains("租户已禁用");
        }
    }
}
