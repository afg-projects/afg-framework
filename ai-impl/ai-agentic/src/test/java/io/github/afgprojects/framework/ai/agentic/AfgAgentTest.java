package io.github.afgprojects.framework.ai.agentic;

import io.github.afgprojects.framework.ai.core.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.agent.AgentStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AfgAgent 单元测试
 */
class AfgAgentTest {

    @Test
    void shouldCreateAgentWithBuilder() {
        // Given
        AfgAgent.LangChain4jDelegate delegate = (input) -> "Response: " + input;

        // When
        AfgAgent agent = AfgAgent.builder()
            .name("TestAgent")
            .description("Test agent for unit testing")
            .delegate(delegate)
            .build();

        // Then
        assertThat(agent.getName()).isEqualTo("TestAgent");
        assertThat(agent.getDescription()).isEqualTo("Test agent for unit testing");
        assertThat(agent.getTools()).isEmpty();
    }

    @Test
    void shouldExecuteAgentAndReturnResponse() {
        // Given
        AfgAgent.LangChain4jDelegate delegate = (input) -> "Echo: " + input;
        AfgAgent agent = AfgAgent.builder()
            .name("EchoAgent")
            .description("Echo agent")
            .delegate(delegate)
            .build();

        // When
        AgentResponse response = agent.execute(
            new AgentRequest("session-1", "Hello")
        );

        // Then
        assertThat(response.status()).isEqualTo(AgentStatus.COMPLETED);
        assertThat(response.output()).isEqualTo("Echo: Hello");
    }

    @Test
    void shouldReturnErrorResponseOnException() {
        // Given
        AfgAgent.LangChain4jDelegate delegate = (input) -> {
            throw new RuntimeException("Test error");
        };
        AfgAgent agent = AfgAgent.builder()
            .name("ErrorAgent")
            .description("Error agent")
            .delegate(delegate)
            .build();

        // When
        AgentResponse response = agent.execute(
            new AgentRequest("session-1", "Test")
        );

        // Then
        assertThat(response.status()).isEqualTo(AgentStatus.ERROR);
        assertThat(response.output()).contains("Test error");
    }
}