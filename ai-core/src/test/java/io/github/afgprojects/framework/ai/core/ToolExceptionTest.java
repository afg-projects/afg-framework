package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.exception.ToolException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ToolException 纯单元测试
 */
@DisplayName("ToolException")
class ToolExceptionTest {

    @Nested
    @DisplayName("构造方法")
    class Constructor {

        @Test
        @DisplayName("应创建带工具名称的异常")
        void shouldCreateException_whenMessageErrorCodeAndToolName() {
            var ex = new ToolException("tool error", "TOOL_001", "web_search");

            assertThat(ex.getMessage()).isEqualTo("tool error");
            assertThat(ex.getErrorCode()).isEqualTo("TOOL_001");
            assertThat(ex.getToolName()).isEqualTo("web_search");
        }

        @Test
        @DisplayName("应创建带原因的异常")
        void shouldCreateException_whenWithCause() {
            var cause = new RuntimeException("http error");
            var ex = new ToolException("tool error", "TOOL_002", "calculator", cause);

            assertThat(ex.getMessage()).isEqualTo("tool error");
            assertThat(ex.getErrorCode()).isEqualTo("TOOL_002");
            assertThat(ex.getToolName()).isEqualTo("calculator");
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("工具名称可为 null")
        void shouldAllowNullToolName() {
            var ex = new ToolException("generic tool error", "TOOL_001", (String) null);

            assertThat(ex.getToolName()).isNull();
        }
    }

    @Nested
    @DisplayName("静态工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("notFound 应创建工具未找到异常")
        void shouldCreateNotFound() {
            var ex = ToolException.notFound("web_search");

            assertThat(ex.getToolName()).isEqualTo("web_search");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.TOOL_NOT_FOUND);
            assertThat(ex.getMessage()).contains("web_search");
        }

        @Test
        @DisplayName("executionFailed 应创建工具执行失败异常")
        void shouldCreateExecutionFailed() {
            var cause = new RuntimeException("NPE");
            var ex = ToolException.executionFailed("calculator", cause);

            assertThat(ex.getToolName()).isEqualTo("calculator");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.TOOL_EXECUTION_FAILED);
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("invalidInput 应创建输入无效异常")
        void shouldCreateInvalidInput() {
            var ex = ToolException.invalidInput("web_search", "query is empty");

            assertThat(ex.getToolName()).isEqualTo("web_search");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.TOOL_INVALID_INPUT);
            assertThat(ex.getMessage()).contains("query is empty");
        }
    }
}
