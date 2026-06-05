package io.github.afgprojects.framework.security.impl.auth;

import io.github.afgprojects.framework.security.auth.tenant.validator.NoOpTenantValidator;
import io.github.afgprojects.framework.security.core.tenant.TenantException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * NoOpTenantValidator 测试
 */
@DisplayName("NoOpTenantValidator 测试")
class NoOpTenantValidatorTest {

    private final NoOpTenantValidator validator = new NoOpTenantValidator();

    @Nested
    @DisplayName("validate 方法")
    class ValidateTests {

        @Test
        @DisplayName("应允许任意租户 ID 通过验证")
        void shouldAllowAnyTenantId() {
            assertThatCode(() -> validator.validate("any-tenant-id"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应允许空字符串租户 ID")
        void shouldAllowEmptyTenantId() {
            assertThatCode(() -> validator.validate(""))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应允许特殊字符租户 ID")
        void shouldAllowSpecialCharsTenantId() {
            assertThatCode(() -> validator.validate("tenant-with-special_chars!@#"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("应从不抛出 TenantException")
        void shouldNeverThrowTenantException() {
            assertThatCode(() -> validator.validate("non-existent-tenant"))
                    .doesNotThrowAnyException();
        }
    }
}