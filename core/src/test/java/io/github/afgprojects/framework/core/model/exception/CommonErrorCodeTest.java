package io.github.afgprojects.framework.core.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommonErrorCodeTest {

    @Nested
    @DisplayName("错误码测试")
    class CodeTests {

        @Test
        @DisplayName("应返回数字错误码")
        void shouldReturnNumericCode() {
            assertThat(CommonErrorCode.FAIL.getCode()).isEqualTo(10001);
            assertThat(CommonErrorCode.PARAM_ERROR.getCode()).isEqualTo(10002);
            assertThat(CommonErrorCode.SYSTEM_ERROR.getCode()).isEqualTo(19000);
        }

        @Test
        @DisplayName("错误码应在定义范围内")
        void shouldBeInRange() {
            for (CommonErrorCode errorCode : CommonErrorCode.values()) {
                assertThat(errorCode.getCode()).isBetween(10001, 19999);
            }
        }

        @Test
        @DisplayName("应返回格式化的错误码字符串")
        void shouldReturnFormattedCode() {
            assertThat(CommonErrorCode.FAIL.formatCode()).isEqualTo("E10001");
            assertThat(CommonErrorCode.PARAM_ERROR.formatCode()).isEqualTo("E10002");
        }
    }

    @Nested
    @DisplayName("错误消息测试")
    class MessageTests {

        @Test
        @DisplayName("应返回错误消息")
        void shouldReturnMessage() {
            assertThat(CommonErrorCode.FAIL.getMessage()).isEqualTo("操作失败");
            assertThat(CommonErrorCode.PARAM_ERROR.getMessage()).isEqualTo("参数错误");
            assertThat(CommonErrorCode.SYSTEM_ERROR.getMessage()).isEqualTo("系统异常");
        }
    }

    @Nested
    @DisplayName("错误分类测试")
    class CategoryTests {

        @Test
        @DisplayName("应返回正确的错误分类")
        void shouldReturnCorrectCategory() {
            assertThat(CommonErrorCode.FAIL.getCategory()).isEqualTo(ErrorCategory.BUSINESS);
            assertThat(CommonErrorCode.UNAUTHORIZED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
            assertThat(CommonErrorCode.CLIENT_TIMEOUT.getCategory()).isEqualTo(ErrorCategory.NETWORK);
            assertThat(CommonErrorCode.SYSTEM_ERROR.getCategory()).isEqualTo(ErrorCategory.SYSTEM);
        }

        @Test
        @DisplayName("安全错误应有 SECURITY 分类")
        void securityErrorsShouldHaveSecurityCategory() {
            assertThat(CommonErrorCode.UNAUTHORIZED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
            assertThat(CommonErrorCode.TOKEN_EXPIRED.getCategory()).isEqualTo(ErrorCategory.SECURITY);
            assertThat(CommonErrorCode.FORBIDDEN.getCategory()).isEqualTo(ErrorCategory.SECURITY);
        }

        @Test
        @DisplayName("网络错误应有 NETWORK 分类")
        void networkErrorsShouldHaveNetworkCategory() {
            assertThat(CommonErrorCode.CLIENT_TIMEOUT.getCategory()).isEqualTo(ErrorCategory.NETWORK);
            assertThat(CommonErrorCode.CLIENT_CONNECT_FAILED.getCategory()).isEqualTo(ErrorCategory.NETWORK);
        }
    }

    @Nested
    @DisplayName("接口实现测试")
    class InterfaceTests {

        @Test
        @DisplayName("应实现 ErrorCode 接口")
        void shouldImplementErrorCode() {
            assertThat(CommonErrorCode.FAIL).isInstanceOf(ErrorCode.class);
        }
    }
}
