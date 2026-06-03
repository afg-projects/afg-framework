package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.exception.AgentException;
import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * AgentException 纯单元测试
 */
@DisplayName("AgentException")
class AgentExceptionTest {

    @Nested
    @DisplayName("构造方法")
    class Constructor {

        @Test
        @DisplayName("应创建带 Agent 名称的异常")
        void shouldCreateException_whenMessageErrorCodeAndAgentName() {
            var ex = new AgentException("agent failed", "AGENT_001", "planner");

            assertThat(ex.getMessage()).isEqualTo("agent failed");
            assertThat(ex.getErrorCode()).isEqualTo("AGENT_001");
            assertThat(ex.getAgentName()).isEqualTo("planner");
        }

        @Test
        @DisplayName("应创建带原因的异常")
        void shouldCreateException_whenWithCause() {
            var cause = new RuntimeException("root cause");
            var ex = new AgentException("agent failed", "AGENT_002", "executor", cause);

            assertThat(ex.getMessage()).isEqualTo("agent failed");
            assertThat(ex.getErrorCode()).isEqualTo("AGENT_002");
            assertThat(ex.getAgentName()).isEqualTo("executor");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("静态工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("iterationExceeded 应创建正确的异常")
        void shouldCreateIterationExceeded() {
            var ex = AgentException.iterationExceeded("planner", 10);

            assertThat(ex.getAgentName()).isEqualTo("planner");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.AGENT_ITERATION_EXCEEDED);
            assertThat(ex.getMessage()).contains("planner").contains("10");
        }

        @Test
        @DisplayName("executionFailed 应创建正确的异常")
        void shouldCreateExecutionFailed() {
            var cause = new RuntimeException("model error");
            var ex = AgentException.executionFailed("worker", cause);

            assertThat(ex.getAgentName()).isEqualTo("worker");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.AGENT_EXECUTION_FAILED);
            assertThat(ex.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("timeout 应创建正确的异常")
        void shouldCreateTimeout() {
            var ex = AgentException.timeout("researcher", 5000);

            assertThat(ex.getAgentName()).isEqualTo("researcher");
            assertThat(ex.getErrorCode()).isEqualTo(AiException.ErrorCodes.AGENT_TIMEOUT);
            assertThat(ex.getMessage()).contains("5000ms");
        }
    }
}
