package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.exception.AgentException;
import io.github.afgprojects.framework.ai.core.api.exception.ToolException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * AI 异常层次结构纯单元测试
 */
@DisplayName("AiException")
class AiExceptionTest {

    @Nested
    @DisplayName("构造方法")
    class Constructor {

        @Test
        @DisplayName("应创建带消息和错误码的异常")
        void shouldCreateException_whenMessageAndErrorCode() {
            var ex = new AiException("connection failed", "LLM_001");

            assertThat(ex.getMessage()).isEqualTo("connection failed");
            assertThat(ex.getErrorCode()).isEqualTo("LLM_001");
            assertThat(ex.getProvider()).isNull();
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("应创建带原因的异常")
        void shouldCreateException_whenMessageErrorCodeAndCause() {
            var cause = new RuntimeException("root cause");
            var ex = new AiException("connection failed", "LLM_001", cause);

            assertThat(ex.getMessage()).isEqualTo("connection failed");
            assertThat(ex.getErrorCode()).isEqualTo("LLM_001");
            assertThat(ex.getCause()).isSameAs(cause);
            assertThat(ex.getProvider()).isNull();
        }

        @Test
        @DisplayName("应创建带提供商信息的异常")
        void shouldCreateException_whenMessageErrorCodeAndProvider() {
            var ex = new AiException("rate limited", "LLM_002", "openai");

            assertThat(ex.getMessage()).isEqualTo("rate limited");
            assertThat(ex.getErrorCode()).isEqualTo("LLM_002");
            assertThat(ex.getProvider()).isEqualTo("openai");
            assertThat(ex.getCause()).isNull();
        }

        @Test
        @DisplayName("应创建带提供商和原因的异常")
        void shouldCreateException_whenAllParameters() {
            var cause = new RuntimeException("timeout");
            var ex = new AiException("timeout", "LLM_004", "anthropic", cause);

            assertThat(ex.getMessage()).isEqualTo("timeout");
            assertThat(ex.getErrorCode()).isEqualTo("LLM_004");
            assertThat(ex.getProvider()).isEqualTo("anthropic");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    @Nested
    @DisplayName("异常层次结构")
    class Hierarchy {

        @Test
        @DisplayName("AiException 应为 RuntimeException")
        void shouldBeRuntimeException() {
            var ex = new AiException("msg", "CODE");

            assertThat(ex).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("AgentException 应为 AiException 子类")
        void shouldAgentExceptionExtendAiException() {
            var ex = new AgentException("agent failed", "AGENT_001", "planner");

            assertThat(ex).isInstanceOf(AiException.class);
        }

        @Test
        @DisplayName("ToolException 应为 AiException 子类")
        void shouldToolExceptionExtendAiException() {
            var ex = new ToolException("tool failed", "TOOL_001", "search");

            assertThat(ex).isInstanceOf(AiException.class);
        }
    }

    @Nested
    @DisplayName("ErrorCodes 常量")
    class ErrorCodes {

        @Test
        @DisplayName("LLM 错误码应有正确值")
        void shouldHaveLlmErrorCodes() {
            assertThat(AiException.ErrorCodes.LLM_CONNECTION_FAILED).isEqualTo("LLM_001");
            assertThat(AiException.ErrorCodes.LLM_RATE_LIMITED).isEqualTo("LLM_002");
            assertThat(AiException.ErrorCodes.LLM_INVALID_RESPONSE).isEqualTo("LLM_003");
            assertThat(AiException.ErrorCodes.LLM_TIMEOUT).isEqualTo("LLM_004");
            assertThat(AiException.ErrorCodes.LLM_API_KEY_INVALID).isEqualTo("LLM_005");
        }

        @Test
        @DisplayName("Tool 错误码应有正确值")
        void shouldHaveToolErrorCodes() {
            assertThat(AiException.ErrorCodes.TOOL_NOT_FOUND).isEqualTo("TOOL_001");
            assertThat(AiException.ErrorCodes.TOOL_EXECUTION_FAILED).isEqualTo("TOOL_002");
            assertThat(AiException.ErrorCodes.TOOL_INVALID_INPUT).isEqualTo("TOOL_003");
        }

        @Test
        @DisplayName("RAG 错误码应有正确值")
        void shouldHaveRagErrorCodes() {
            assertThat(AiException.ErrorCodes.RAG_INDEX_FAILED).isEqualTo("RAG_001");
            assertThat(AiException.ErrorCodes.RAG_SEARCH_FAILED).isEqualTo("RAG_002");
            assertThat(AiException.ErrorCodes.RAG_DOCUMENT_LOAD_FAILED).isEqualTo("RAG_003");
        }

        @Test
        @DisplayName("Agent 错误码应有正确值")
        void shouldHaveAgentErrorCodes() {
            assertThat(AiException.ErrorCodes.AGENT_ITERATION_EXCEEDED).isEqualTo("AGENT_001");
            assertThat(AiException.ErrorCodes.AGENT_EXECUTION_FAILED).isEqualTo("AGENT_002");
            assertThat(AiException.ErrorCodes.AGENT_TIMEOUT).isEqualTo("AGENT_003");
        }

        @Test
        @DisplayName("MultiAgent 错误码应有正确值")
        void shouldHaveMultiAgentErrorCodes() {
            assertThat(AiException.ErrorCodes.MULTIAGENT_COORDINATION_FAILED).isEqualTo("MULTI_001");
            assertThat(AiException.ErrorCodes.MULTIAGENT_AGENT_NOT_FOUND).isEqualTo("MULTI_002");
            assertThat(AiException.ErrorCodes.MULTIAGENT_CONFLICT_UNRESOLVED).isEqualTo("MULTI_003");
        }

        @Test
        @DisplayName("Config 错误码应有正确值")
        void shouldHaveConfigErrorCodes() {
            assertThat(AiException.ErrorCodes.CONFIG_INVALID).isEqualTo("CONFIG_001");
            assertThat(AiException.ErrorCodes.CONFIG_MISSING).isEqualTo("CONFIG_002");
        }
    }
}
