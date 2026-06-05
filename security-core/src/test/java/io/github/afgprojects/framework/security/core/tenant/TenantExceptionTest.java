package io.github.afgprojects.framework.security.core.tenant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TenantException 测试
 */
@DisplayName("TenantException 测试")
class TenantExceptionTest {

    @Nested
    @DisplayName("构造函数")
    class ConstructorTests {

        @Test
        @DisplayName("带错误码应正确创建")
        void shouldCreateWithErrorCode() {
            TenantException exception = new TenantException(TenantErrorCode.TENANT_NOT_FOUND, "租户不存在: tenant-001");

            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_NOT_FOUND);
            assertThat(exception.getMessage()).contains("租户不存在: tenant-001");
        }

        @Test
        @DisplayName("带错误码和原因应正确创建")
        void shouldCreateWithErrorCodeAndCause() {
            Throwable cause = new RuntimeException("Database error");
            TenantException exception = new TenantException(TenantErrorCode.TENANT_CONFIG_ERROR, "配置错误", cause);

            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_CONFIG_ERROR);
            assertThat(exception.getMessage()).contains("配置错误");
            assertThat(exception.getCause()).isEqualTo(cause);
        }

        @Test
        @DisplayName("带错误码无消息应正确创建")
        void shouldCreateWithErrorCodeOnly() {
            TenantException exception = new TenantException(TenantErrorCode.TENANT_ACCESS_DENIED);

            assertThat(exception.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_ACCESS_DENIED);
            assertThat(exception.getMessage()).isNotNull();
        }
    }

    @Nested
    @DisplayName("异常行为")
    class ExceptionBehaviorTests {

        @Test
        @DisplayName("应可被抛出和捕获")
        void shouldBeThrowableAndCatchable() {
            assertThatThrownBy(() -> {
                throw new TenantException(TenantErrorCode.TENANT_NOT_FOUND, "租户不存在");
            })
                    .isInstanceOf(TenantException.class)
                    .hasMessageContaining("租户不存在");
        }

        @Test
        @DisplayName("应可被 RuntimeException 捕获")
        void shouldBeCatchableAsRuntimeException() {
            assertThatThrownBy(() -> {
                throw new TenantException(TenantErrorCode.TENANT_DISABLED, "租户已禁用");
            })
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("getErrorCode")
    class GetErrorCodeTests {

        @Test
        @DisplayName("应返回正确的错误码")
        void shouldReturnCorrectErrorCode() {
            TenantException ex1 = new TenantException(TenantErrorCode.TENANT_NOT_FOUND, "not found");
            TenantException ex2 = new TenantException(TenantErrorCode.TENANT_DISABLED, "disabled");
            TenantException ex3 = new TenantException(TenantErrorCode.TENANT_EXPIRED, "expired");

            assertThat(ex1.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_NOT_FOUND);
            assertThat(ex2.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_DISABLED);
            assertThat(ex3.getErrorCode()).isEqualTo(TenantErrorCode.TENANT_EXPIRED);
        }
    }
}
