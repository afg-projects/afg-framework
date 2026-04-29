package io.github.afgprojects.framework.core.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.model.exception.ErrorCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DataScopeErrorCode 测试
 */
@DisplayName("DataScopeErrorCode 测试")
class DataScopeErrorCodeTest {

    @Nested
    @DisplayName("错误码范围测试")
    class CodeRangeTests {

        @Test
        @DisplayName("错误码应该在 18000-18999 范围内")
        void shouldHaveCorrectCodeRange() {
            for (DataScopeErrorCode errorCode : DataScopeErrorCode.values()) {
                assertThat(errorCode.getCode()).isBetween(18000, 18999);
            }
        }

        @Test
        @DisplayName("错误码应该唯一")
        void shouldHaveUniqueCodes() {
            long distinctCount = java.util.Arrays.stream(DataScopeErrorCode.values())
                    .map(DataScopeErrorCode::getCode)
                    .distinct()
                    .count();

            assertThat(distinctCount).isEqualTo(DataScopeErrorCode.values().length);
        }
    }

    @Nested
    @DisplayName("错误码属性测试")
    class CodePropertyTests {

        @Test
        @DisplayName("CONFIG_ERROR 应该是系统错误")
        void configErrorShouldBeSystemError() {
            assertThat(DataScopeErrorCode.CONFIG_ERROR.getCode()).isEqualTo(18000);
            assertThat(DataScopeErrorCode.CONFIG_ERROR.getMessage()).isEqualTo("数据权限配置错误");
            assertThat(DataScopeErrorCode.CONFIG_ERROR.getCategory())
                    .isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("ACCESS_DENIED 应该是安全错误")
        void accessDeniedShouldBeSecurityError() {
            assertThat(DataScopeErrorCode.ACCESS_DENIED.getCode()).isEqualTo(18005);
            assertThat(DataScopeErrorCode.ACCESS_DENIED.getMessage()).isEqualTo("无权访问该数据");
            assertThat(DataScopeErrorCode.ACCESS_DENIED.getCategory())
                    .isEqualTo(ErrorCategory.SECURITY);
        }
    }
}