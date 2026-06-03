package io.github.afgprojects.framework.ai.core;

import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * AgentRequest 纯单元测试
 */
@DisplayName("AgentRequest")
class AgentRequestTest {

    @Nested
    @DisplayName("简化构造方法")
    class SimplifiedConstructor {

        @Test
        @DisplayName("应创建带会话 ID 和用户输入的请求")
        void shouldCreateRequest_whenSessionIdAndUserInput() {
            var request = new AgentRequest("session-1", "Hello, agent!");

            assertThat(request.sessionId()).isEqualTo("session-1");
            assertThat(request.userInput()).isEqualTo("Hello, agent!");
            assertThat(request.context()).isEmpty();
            assertThat(request.history()).isEmpty();
        }

        @Test
        @DisplayName("应创建带上下文的请求")
        void shouldCreateRequest_whenWithContext() {
            var context = Map.<String, Object>of("key", "value");
            var request = new AgentRequest("session-1", "Hello", context);

            assertThat(request.sessionId()).isEqualTo("session-1");
            assertThat(request.userInput()).isEqualTo("Hello");
            assertThat(request.context()).containsEntry("key", "value");
            assertThat(request.history()).isEmpty();
        }
    }

    @Nested
    @DisplayName("完整构造方法")
    class FullConstructor {

        @Test
        @DisplayName("应创建带所有参数的请求")
        void shouldCreateRequest_whenAllParameters() {
            var context = Map.<String, Object>of("temperature", 0.7);
            var history = List.<Object>of("previous message");
            var request = new AgentRequest("session-1", "Hello", context, history);

            assertThat(request.sessionId()).isEqualTo("session-1");
            assertThat(request.userInput()).isEqualTo("Hello");
            assertThat(request.context()).containsEntry("temperature", 0.7);
            assertThat(request.history()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("record 相等性")
    class RecordEquality {

        @Test
        @DisplayName("相同参数的请求应相等")
        void shouldBeEqual_whenSameParameters() {
            var request1 = new AgentRequest("s1", "Hello");
            var request2 = new AgentRequest("s1", "Hello");

            assertThat(request1).isEqualTo(request2);
            assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        }

        @Test
        @DisplayName("不同参数的请求应不等")
        void shouldNotBeEqual_whenDifferentParameters() {
            var request1 = new AgentRequest("s1", "Hello");
            var request2 = new AgentRequest("s2", "Hello");

            assertThat(request1).isNotEqualTo(request2);
        }
    }
}
