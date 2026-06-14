package io.github.afgprojects.framework.security.resource.permission;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PermissionDeniedException 测试
 */
@DisplayName("PermissionDeniedException 测试")
class PermissionDeniedExceptionTest {

    @Nested
    @DisplayName("异常创建")
    class ExceptionCreationTests {

        @Test
        @DisplayName("应包含错误消息")
        void shouldContainMessage() {
            String message = "权限不足：需要 user:delete 权限";

            PermissionDeniedException exception = new PermissionDeniedException(message);

            assertThat(exception.getMessage()).isEqualTo(message);
        }

        @Test
        @DisplayName("应为 RuntimeException 的子类")
        void shouldBeRuntimeException() {
            PermissionDeniedException exception = new PermissionDeniedException("test");

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("可以正常抛出和捕获")
        void shouldThrowAndCatch() {
            assertThatThrownBy(() -> {
                throw new PermissionDeniedException("Access denied");
            })
                    .isInstanceOf(PermissionDeniedException.class)
                    .hasMessage("Access denied");
        }
    }
}
