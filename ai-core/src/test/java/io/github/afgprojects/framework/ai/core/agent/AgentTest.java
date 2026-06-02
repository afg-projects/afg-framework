package io.github.afgprojects.framework.ai.core.api.agent.

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Agent 核心接口测试
 *
 * @author afg-projects
 * @since 1.0.0
 */
class AgentTest {

    @Test
    void agentRequest_shouldCreateWithAllFields() {
        AgentRequest request = new AgentRequest(
            "session-123",
            "Hello, Agent!",
            Map.of("key", "value"),
            List.of()
        );

        assertEquals("session-123", request.sessionId());
        assertEquals("Hello, Agent!", request.userInput());
        assertEquals("value", request.context().get("key"));
        assertTrue(request.history().isEmpty());
    }

    @Test
    void agentResponse_shouldCreateWithAllFields() {
        AgentResponse response = new AgentResponse(
            "Hello!",
            List.of(),
            AgentStatus.COMPLETED,
            Map.of()
        );

        assertEquals("Hello!", response.output());
        assertEquals(AgentStatus.COMPLETED, response.status());
        assertTrue(response.toolCalls().isEmpty());
        assertTrue(response.metadata().isEmpty());
    }

    @Test
    void agentStatus_shouldHaveAllValues() {
        AgentStatus[] statuses = AgentStatus.values();
        assertEquals(4, statuses.length);
        assertEquals(AgentStatus.COMPLETED, AgentStatus.valueOf("COMPLETED"));
        assertEquals(AgentStatus.NEEDS_INPUT, AgentStatus.valueOf("NEEDS_INPUT"));
        assertEquals(AgentStatus.TOOL_CALLING, AgentStatus.valueOf("TOOL_CALLING"));
        assertEquals(AgentStatus.ERROR, AgentStatus.valueOf("ERROR"));
    }

    @Test
    void agent_shouldDefineCoreMethods() {
        // Test that Agent interface has the required methods
        // This is a compile-time check - if the interface doesn't have these methods,
        // the test class won't compile
        Agent testAgent = new TestAgentImpl();

        assertEquals("TestAgent", testAgent.getName());
        assertEquals("A test agent for unit testing", testAgent.getDescription());
        assertNotNull(testAgent.getTools());
    }

    @Test
    void agentExecutor_shouldDefineExecuteMethods() {
        // Test that AgentExecutor interface has the required methods
        AgentExecutor executor = new TestAgentExecutorImpl();

        assertNotNull(executor);
    }

    // Test implementation of Agent interface
    private static class TestAgentImpl implements Agent {
        @Override
        public String getName() {
            return "TestAgent";
        }

        @Override
        public String getDescription() {
            return "A test agent for unit testing";
        }

        @Override
        public AgentResponse execute(AgentRequest request) {
            return new AgentResponse(
                "Test response",
                List.of(),
                AgentStatus.COMPLETED,
                Map.of()
            );
        }

        @Override
        public List<?> getTools() {
            return List.of();
        }
    }

    // Test implementation of AgentExecutor interface
    private static class TestAgentExecutorImpl implements AgentExecutor {
        @Override
        public AgentResponse execute(Agent agent, AgentRequest request) {
            return agent.execute(request);
        }

        @Override
        public AgentResponse executeWithRetry(Agent agent, AgentRequest request, int maxRetries) {
            return agent.execute(request);
        }
    }
}
