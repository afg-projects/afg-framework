package io.github.afgprojects.framework.core.module.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ModuleException 单元测试
 */
@DisplayName("ModuleException 单元测试")
class ModuleExceptionTest {

    @Nested
    @DisplayName("构造函数测试")
    class ConstructorTests {

        @Test
        @DisplayName("应该使用 code 和 message 创建异常")
        void shouldCreateWithCodeAndMessage() {
            // when
            ModuleException exception = new ModuleException(1001, "Module error");

            // then
            assertThat(exception.getCode()).isEqualTo(1001);
            assertThat(exception.getMessage()).isEqualTo("Module error");
            assertThat(exception.getModuleId()).isNull();
        }

        @Test
        @DisplayName("应该使用 code、message 和 moduleId 创建异常")
        void shouldCreateWithCodeMessageAndModuleId() {
            // when
            ModuleException exception = new ModuleException(1001, "Module error", "test-module");

            // then
            assertThat(exception.getCode()).isEqualTo(1001);
            assertThat(exception.getMessage()).isEqualTo("Module error");
            assertThat(exception.getModuleId()).isEqualTo("test-module");
        }

        @Test
        @DisplayName("应该使用 code、message 和 cause 创建异常")
        void shouldCreateWithCodeMessageAndCause() {
            // given
            Throwable cause = new RuntimeException("Root cause");

            // when
            ModuleException exception = new ModuleException(1001, "Module error", cause);

            // then
            assertThat(exception.getCode()).isEqualTo(1001);
            assertThat(exception.getMessage()).isEqualTo("Module error");
            assertThat(exception.getCause()).isEqualTo(cause);
            assertThat(exception.getModuleId()).isNull();
        }

        @Test
        @DisplayName("应该使用所有参数创建异常")
        void shouldCreateWithAllParameters() {
            // given
            Throwable cause = new RuntimeException("Root cause");

            // when
            ModuleException exception = new ModuleException(1001, "Module error", "test-module", cause);

            // then
            assertThat(exception.getCode()).isEqualTo(1001);
            assertThat(exception.getMessage()).isEqualTo("Module error");
            assertThat(exception.getModuleId()).isEqualTo("test-module");
            assertThat(exception.getCause()).isEqualTo(cause);
        }
    }
}
