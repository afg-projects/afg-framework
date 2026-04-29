package io.github.afgprojects.framework.core.model.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.exception.AfgException;

class BusinessExceptionTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("使用消息构造")
        void shouldCreateWithMessage() {
            BusinessException ex = new BusinessException("业务异常");

            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.FAIL.getCode());
            assertThat(ex.getMessage()).isEqualTo("业务异常");
            assertThat(ex.getBusinessMessage()).isEqualTo("业务异常");
        }

        @Test
        @DisplayName("使用错误码构造")
        void shouldCreateWithErrorCode() {
            BusinessException ex = new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND);

            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND.getCode());
            assertThat(ex.getMessage()).isEqualTo("实体不存在");
        }

        @Test
        @DisplayName("使用错误码和自定义消息构造")
        void shouldCreateWithErrorCodeAndMessage() {
            BusinessException ex = new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "用户ID=123不存在");

            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.ENTITY_NOT_FOUND.getCode());
            assertThat(ex.getMessage()).isEqualTo("用户ID=123不存在");
        }

        @Test
        @DisplayName("使用错误码、消息和原因构造")
        void shouldCreateWithErrorCodeMessageAndCause() {
            RuntimeException cause = new RuntimeException("数据库异常");
            BusinessException ex = new BusinessException(CommonErrorCode.QUERY_ERROR, "查询失败", cause);

            assertThat(ex.getCode()).isEqualTo(CommonErrorCode.QUERY_ERROR.getCode());
            assertThat(ex.getMessage()).isEqualTo("查询失败");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("继承关系测试")
    class InheritanceTests {

        @Test
        @DisplayName("应继承 AfgException")
        void shouldBeAfgException() {
            BusinessException ex = new BusinessException("test");

            assertThat(ex).isInstanceOf(AfgException.class);
        }

        @Test
        @DisplayName("应继承 RuntimeException")
        void shouldBeRuntimeException() {
            BusinessException ex = new BusinessException("test");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("格式化测试")
    class FormatTests {

        @Test
        @DisplayName("应返回格式化的错误码")
        void shouldReturnFormattedCode() {
            BusinessException ex = new BusinessException(CommonErrorCode.PARAM_ERROR);

            assertThat(ex.formatCode()).isEqualTo("E10002");
        }
    }
}
