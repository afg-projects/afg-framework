package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.agent.AgentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AgentResponse 纯单元测试
 */
@DisplayName("AgentResponse")
class AgentResponseTest {

    @Nested
    @DisplayName("completed 工厂方法")
    class Completed {

        @Test
        @DisplayName("应创建完成状态的响应")
        void shouldCreateCompletedResponse() {
            var response = AgentResponse.completed("Task finished");

            assertThat(response.output()).isEqualTo("Task finished");
            assertThat(response.status()).isEqualTo(AgentStatus.COMPLETED);
            assertThat(response.toolCalls()).isEmpty();
            assertThat(response.metadata()).isEmpty();
            assertThat(response.isCompleted()).isTrue();
            assertThat(response.needsInput()).isFalse();
            assertThat(response.isToolCalling()).isFalse();
            assertThat(response.isError()).isFalse();
        }

        @Test
        @DisplayName("output 可为 null")
        void shouldAllowNullOutput() {
            var response = AgentResponse.completed(null);

            assertThat(response.output()).isNull();
            assertThat(response.isCompleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("needsInput 工厂方法")
    class NeedsInput {

        @Test
        @DisplayName("应创建需要输入的响应")
        void shouldCreateNeedsInputResponse() {
            var response = AgentResponse.needsInput("Please provide more details");

            assertThat(response.output()).isEqualTo("Please provide more details");
            assertThat(response.status()).isEqualTo(AgentStatus.NEEDS_INPUT);
            assertThat(response.needsInput()).isTrue();
            assertThat(response.isCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("toolCalling 工厂方法")
    class ToolCalling {

        @Test
        @DisplayName("应创建工具调用状态的响应")
        void shouldCreateToolCallingResponse() {
            var toolCalls = List.<Object>of("tool_call_1", "tool_call_2");
            var response = AgentResponse.toolCalling(toolCalls);

            assertThat(response.output()).isNull();
            assertThat(response.status()).isEqualTo(AgentStatus.TOOL_CALLING);
            assertThat(response.toolCalls()).hasSize(2);
            assertThat(response.isToolCalling()).isTrue();
        }
    }

    @Nested
    @DisplayName("error 工厂方法")
    class Error {

        @Test
        @DisplayName("应创建错误响应")
        void shouldCreateErrorResponse() {
            var response = AgentResponse.error("Something went wrong");

            assertThat(response.output()).isEqualTo("Something went wrong");
            assertThat(response.status()).isEqualTo(AgentStatus.ERROR);
            assertThat(response.metadata()).containsEntry("error", true);
            assertThat(response.isError()).isTrue();
        }

        @Test
        @DisplayName("应创建带异常的错误响应")
        void shouldCreateErrorResponseWithThrowable() {
            var throwable = new RuntimeException("boom");
            var response = AgentResponse.error("Failed", throwable);

            assertThat(response.status()).isEqualTo(AgentStatus.ERROR);
            assertThat(response.metadata()).containsEntry("error", true);
            assertThat(response.metadata()).containsEntry("exception", throwable);
        }
    }

    @Nested
    @DisplayName("状态判断方法")
    class StatusChecks {

        @Test
        @DisplayName("不同状态的响应应正确判断")
        void shouldCorrectlyJudgeStatus() {
            var completed = AgentResponse.completed("done");
            var needsInput = AgentResponse.needsInput("prompt");
            var toolCalling = AgentResponse.toolCalling(List.of());
            var error = AgentResponse.error("fail");

            assertThat(completed.isCompleted()).isTrue();
            assertThat(needsInput.needsInput()).isTrue();
            assertThat(toolCalling.isToolCalling()).isTrue();
            assertThat(error.isError()).isTrue();

            assertThat(completed.needsInput()).isFalse();
            assertThat(completed.isToolCalling()).isFalse();
            assertThat(completed.isError()).isFalse();
        }
    }
}
