package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.agent.AgentStatus;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.AgentNode;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultAgentNodeTest {

    private Agent mockAgent;
    private DefaultAgentNode agentNode;

    @BeforeEach
    void setUp() {
        mockAgent = mock(Agent.class);
        when(mockAgent.getName()).thenReturn("test-agent");
        when(mockAgent.getDescription()).thenReturn("Test agent description");
        when(mockAgent.getTools()).thenReturn(List.of());
        when(mockAgent.execute(any(AgentRequest.class)))
                .thenReturn(AgentResponse.completed("test output"));

        agentNode = new DefaultAgentNode("node-1", mockAgent);
    }

    @Test
    @DisplayName("获取节点ID")
    void getId_returnsId() {
        assertThat(agentNode.getId()).isEqualTo("node-1");
    }

    @Test
    @DisplayName("获取节点类型")
    void getType_returnsAgent() {
        assertThat(agentNode.getType()).isEqualTo(NodeType.AGENT);
    }

    @Test
    @DisplayName("获取绑定的 Agent")
    void getAgent_returnsAgent() {
        assertThat(agentNode.getAgent()).isSameAs(mockAgent);
    }

    @Test
    @DisplayName("获取默认执行模式")
    void getExecutionMode_defaultIsSync() {
        assertThat(agentNode.getExecutionMode()).isEqualTo(AgentNode.ExecutionMode.SYNC);
    }

    @Test
    @DisplayName("获取默认工具列表")
    void getToolNames_defaultIsEmpty() {
        assertThat(agentNode.getToolNames()).isEmpty();
    }

    @Test
    @DisplayName("执行节点成功")
    void execute_shouldCallAgent() {
        WorkflowState state = WorkflowState.empty()
                .with("_workflowId", "wf-1");

        NodeResult result = agentNode.execute(state);

        assertThat(result.status().name()).isEqualTo("SUCCESS");
        verify(mockAgent).execute(any(AgentRequest.class));
    }

    @Test
    @DisplayName("执行节点失败时返回失败结果")
    void execute_whenException_returnsFailure() {
        when(mockAgent.execute(any(AgentRequest.class)))
                .thenThrow(new RuntimeException("Agent error"));

        WorkflowState state = WorkflowState.empty()
                .with("_workflowId", "wf-1");

        NodeResult result = agentNode.execute(state);

        assertThat(result.status().name()).isEqualTo("FAILURE");
        assertThat(result.errorMessage()).contains("Agent execution failed");
    }

    @Test
    @DisplayName("构建请求从状态获取输入")
    void buildRequest_extractsInputFromState() {
        WorkflowState state = WorkflowState.empty()
                .with("_workflowId", "wf-1")
                .with("user_input", "hello world");

        DefaultAgentNode node = new DefaultAgentNode("node-1", mockAgent, "user_input", "result");

        AgentRequest request = node.buildRequest(state);

        assertThat(request.userInput()).isEqualTo("hello world");
        assertThat(request.sessionId()).isEqualTo("wf-1");
    }

    @Test
    @DisplayName("构建请求使用默认值当输入不存在")
    void buildRequest_usesDefaultWhenInputMissing() {
        WorkflowState state = WorkflowState.empty();

        AgentRequest request = agentNode.buildRequest(state);

        assertThat(request.userInput()).isEmpty();
        assertThat(request.sessionId()).isEqualTo("unknown-session");
    }

    @Test
    @DisplayName("处理响应写入状态")
    void processResponse_writesToState() {
        WorkflowState state = WorkflowState.empty();
        AgentResponse response = AgentResponse.completed("agent response");
        DefaultAgentNode node = new DefaultAgentNode("node-1", mockAgent, "input", "result");

        WorkflowState updated = node.processResponse(response, state);

        assertThat((String) updated.get("node-1_result")).isEqualTo("agent response");
        assertThat((String) updated.get("node-1_status")).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("处理响应包含工具调用")
    void processResponse_includesToolCalls() {
        WorkflowState state = WorkflowState.empty();
        AgentResponse response = AgentResponse.toolCalling(List.of("tool1", "tool2"));
        DefaultAgentNode node = new DefaultAgentNode("node-1", mockAgent, "input", "result");

        WorkflowState updated = node.processResponse(response, state);

        List<?> toolCalls = updated.get("node-1_toolCalls");
        assertThat(toolCalls).isNotNull();
        assertThat(toolCalls).hasSize(2);
    }

    @Test
    @DisplayName("使用自定义系统提示")
    void withSystemPrompt_usesPrompt() {
        DefaultAgentNode node = new DefaultAgentNode(
                "node-1",
                mockAgent,
                "input",
                "output",
                "Custom prompt"
        );

        assertThat(node.getSystemPrompt()).isEqualTo("Custom prompt");
    }

    @Test
    @DisplayName("使用自定义输入输出键")
    void customInputOutputKeys() {
        DefaultAgentNode node = new DefaultAgentNode(
                "node-1",
                mockAgent,
                "custom_input",
                "custom_output"
        );

        assertThat(node.getInputKey()).isEqualTo("custom_input");
        assertThat(node.getOutputKey()).isEqualTo("custom_output");
    }

    @Test
    @DisplayName("使用自定义工具列表")
    void customToolNames() {
        DefaultAgentNode node = new DefaultAgentNode(
                "node-1",
                mockAgent,
                "input",
                "output",
                null,
                List.of("tool1", "tool2"),
                AgentNode.ExecutionMode.SYNC
        );

        assertThat(node.getToolNames()).containsExactly("tool1", "tool2");
    }

    @Test
    @DisplayName("使用异步执行模式")
    void asyncExecutionMode() {
        DefaultAgentNode node = new DefaultAgentNode(
                "node-1",
                mockAgent,
                "input",
                "output",
                null,
                List.of(),
                AgentNode.ExecutionMode.ASYNC
        );

        assertThat(node.getExecutionMode()).isEqualTo(AgentNode.ExecutionMode.ASYNC);
    }

    @Test
    @DisplayName("使用流式执行模式")
    void streamingExecutionMode() {
        DefaultAgentNode node = new DefaultAgentNode(
                "node-1",
                mockAgent,
                "input",
                "output",
                null,
                List.of(),
                AgentNode.ExecutionMode.STREAMING
        );

        assertThat(node.getExecutionMode()).isEqualTo(AgentNode.ExecutionMode.STREAMING);
    }

    @Test
    @DisplayName("完整工作流执行")
    void fullWorkflowExecution() {
        WorkflowState state = WorkflowState.empty()
                .with("_workflowId", "wf-1")
                .with("user_input", "What is the weather?");

        DefaultAgentNode node = new DefaultAgentNode(
                "weather-agent",
                mockAgent,
                "user_input",
                "response"
        );

        when(mockAgent.execute(any(AgentRequest.class)))
                .thenReturn(AgentResponse.completed("The weather is sunny."));

        NodeResult result = node.execute(state);

        assertThat(result.status().name()).isEqualTo("SUCCESS");
        assertThat((String) result.updatedState().get("weather-agent_response"))
                .isEqualTo("The weather is sunny.");
    }

    @Test
    @DisplayName("响应状态正确写入")
    void responseStatusWrittenToState() {
        WorkflowState state = WorkflowState.empty();
        AgentResponse response = AgentResponse.needsInput("Please provide more info");
        DefaultAgentNode node = new DefaultAgentNode("node-1", mockAgent, "input", "result");

        WorkflowState updated = node.processResponse(response, state);

        assertThat((String) updated.get("node-1_status")).isEqualTo("NEEDS_INPUT");
    }

    @Test
    @DisplayName("错误响应正确处理")
    void errorResponseHandling() {
        WorkflowState state = WorkflowState.empty();
        AgentResponse response = AgentResponse.error("Something went wrong");
        DefaultAgentNode node = new DefaultAgentNode("node-1", mockAgent, "input", "result");

        WorkflowState updated = node.processResponse(response, state);

        assertThat((String) updated.get("node-1_status")).isEqualTo("ERROR");
        assertThat((String) updated.get("node-1_result")).isEqualTo("Something went wrong");
    }
}
