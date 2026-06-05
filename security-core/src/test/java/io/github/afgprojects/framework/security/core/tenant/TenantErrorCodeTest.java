package io.github.afgprojects.framework.security.core.tenant;

import io.github.afgprojects.framework.commons.exception.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TenantErrorCode 测试
 */
@DisplayName("TenantErrorCode 测试")
class TenantErrorCodeTest {

    @Nested
    @DisplayName("枚举值完整性")
    class EnumValueTests {

        @Test
        @DisplayName("应包含所有错误码")
        void shouldContainAllErrorCodes() {
            TenantErrorCode[] codes = TenantErrorCode.values();

            assertThat(codes).hasSize(11);
            assertThat(codes).containsExactlyInAnyOrder(
                    TenantErrorCode.TENANT_NOT_FOUND,
                    TenantErrorCode.TENANT_DISABLED,
                    TenantErrorCode.TENANT_EXPIRED,
                    TenantErrorCode.TENANT_SUSPENDED,
                    TenantErrorCode.TENANT_UNRESOLVED,
                    TenantErrorCode.TENANT_ACCESS_DENIED,
                    TenantErrorCode.TENANT_SWITCH_FAILED,
                    TenantErrorCode.TENANT_INVALID_PARAM,
                    TenantErrorCode.TENANT_CONFIG_ERROR,
                    TenantErrorCode.TENANT_DATASOURCE_UNAVAILABLE,
                    TenantErrorCode.TENANT_INIT_FAILED
            );
        }
    }

    @Nested
    @DisplayName("错误码属性")
    class ErrorCodePropertyTests {

        @Test
        @DisplayName("TENANT_NOT_FOUND 应有正确的属性")
        void shouldHaveCorrectPropertiesForNotFound() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getCode()).isEqualTo(20000);
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getMessage()).isNotNull();
            assertThat(TenantErrorCode.TENANT_NOT_FOUND.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("TENANT_DISABLED 应有正确的属性")
        void shouldHaveCorrectPropertiesForDisabled() {
            assertThat(TenantErrorCode.TENANT_DISABLED.getCode()).isEqualTo(20001);
            assertThat(TenantErrorCode.TENANT_DISABLED.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("TENANT_EXPIRED 应有正确的属性")
        void shouldHaveCorrectPropertiesForExpired() {
            assertThat(TenantErrorCode.TENANT_EXPIRED.getCode()).isEqualTo(20002);
            assertThat(TenantErrorCode.TENANT_EXPIRED.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("TENANT_SUSPENDED 应有正确的属性")
        void shouldHaveCorrectPropertiesForSuspended() {
            assertThat(TenantErrorCode.TENANT_SUSPENDED.getCode()).isEqualTo(20010);
            assertThat(TenantErrorCode.TENANT_SUSPENDED.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("TENANT_UNRESOLVED 应有正确的属性")
        void shouldHaveCorrectPropertiesForUnresolved() {
            assertThat(TenantErrorCode.TENANT_UNRESOLVED.getCode()).isEqualTo(20003);
            assertThat(TenantErrorCode.TENANT_UNRESOLVED.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
        }

        @Test
        @DisplayName("TENANT_ACCESS_DENIED 应属于 SECURITY 类别")
        void shouldBelongToSecurityCategory() {
            assertThat(TenantErrorCode.TENANT_ACCESS_DENIED.getCode()).isEqualTo(20004);
            assertThat(TenantErrorCode.TENANT_ACCESS_DENIED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("TENANT_CONFIG_ERROR 应属于 SYSTEM 类别")
        void shouldBelongToSystemCategory() {
            assertThat(TenantErrorCode.TENANT_CONFIG_ERROR.getCode()).isEqualTo(20007);
            assertThat(TenantErrorCode.TENANT_CONFIG_ERROR.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("TENANT_DATASOURCE_UNAVAILABLE 应属于 SYSTEM 类别")
        void shouldBelongToSystemCategoryForDatasource() {
            assertThat(TenantErrorCode.TENANT_DATASOURCE_UNAVAILABLE.getCode()).isEqualTo(20008);
            assertThat(TenantErrorCode.TENANT_DATASOURCE_UNAVAILABLE.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("TENANT_INIT_FAILED 应属于 SYSTEM 类别")
        void shouldBelongToSystemCategoryForInitFailed() {
            assertThat(TenantErrorCode.TENANT_INIT_FAILED.getCode()).isEqualTo(20009);
            assertThat(TenantErrorCode.TENANT_INIT_FAILED.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }
    }

    @Nested
    @DisplayName("错误码范围")
    class ErrorCodeRangeTests {

        @Test
        @DisplayName("所有错误码应在 20000-20999 范围内")
        void shouldBeInRange() {
            for (TenantErrorCode code : TenantErrorCode.values()) {
                assertThat(code.getCode()).isBetween(20000, 20999);
            }
        }

        @Test
        @DisplayName("所有错误码应唯一")
        void shouldBeUnique() {
            long distinctCount = java.util.Arrays.stream(TenantErrorCode.values())
                    .map(TenantErrorCode::getCode)
                    .distinct()
                    .count();

            assertThat(distinctCount).isEqualTo(TenantErrorCode.values().length);
        }
    }

    @Nested
    @DisplayName("ErrorCode 接口")
    class ErrorCodeInterfaceTests {

        @Test
        @DisplayName("应实现 ErrorCode 接口")
        void shouldImplementErrorCode() {
            assertThat(TenantErrorCode.TENANT_NOT_FOUND)
                    .isInstanceOf(io.github.afgprojects.framework.commons.exception.ErrorCode.class);
        }
    }
}
