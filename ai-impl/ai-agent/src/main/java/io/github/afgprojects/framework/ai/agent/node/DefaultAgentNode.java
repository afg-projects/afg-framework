package io.github.afgprojects.framework.ai.agent.node;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeResult;
import io.github.afgprojects.framework.ai.core.api.multiagent.graph.NodeType;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.AgentNode;
import io.github.afgprojects.framework.ai.core.api.multiagent.state.WorkflowState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 默认 Agent 节点实现
 *
 * <p>DefaultAgentNode 是 AgentNode 的标准实现，提供：
 * <ul>
 *   <li>从状态中提取输入</li>
 *   <li>调用 Agent 执行</li>
 *   <li>将输出写入状态</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * // 创建基本 Agent 节点
 * AgentNode node = new DefaultAgentNode("my-agent", myAgent);
 *
 * // 创建带自定义输入输出键的节点
 * AgentNode node = new DefaultAgentNode("my-agent", myAgent, "input", "output");
 *
 * // 创建带系统提示的节点
 * AgentNode node = new DefaultAgentNode("my-agent", myAgent, "input", "output", "Custom system prompt");
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultAgentNode implements AgentNode {

    private final String id;
    private final Agent agent;
    private final String inputKey;
    private final String outputKey;
    private final String systemPrompt;
    private final List<String> toolNames;
    private final ExecutionMode executionMode;

    /**
     * 创建 Agent 节点
     *
     * @param id    节点 ID
     * @param agent 绑定的 Agent
     */
    public DefaultAgentNode(String id, Agent agent) {
        this(id, agent, "user_input", "output", null, List.of(), ExecutionMode.SYNC);
    }

    /**
     * 创建 Agent 节点
     *
     * @param id        节点 ID
     * @param agent     绑定的 Agent
     * @param inputKey  输入键（从状态中获取输入）
     * @param outputKey 输出键（将输出写入状态）
     */
    public DefaultAgentNode(String id, Agent agent, String inputKey, String outputKey) {
        this(id, agent, inputKey, outputKey, null, List.of(), ExecutionMode.SYNC);
    }

    /**
     * 创建 Agent 节点
     *
     * @param id           节点 ID
     * @param agent        绑定的 Agent
     * @param inputKey     输入键（从状态中获取输入）
     * @param outputKey    输出键（将输出写入状态）
     * @param systemPrompt 系统提示（可选）
     */
    public DefaultAgentNode(String id, Agent agent, String inputKey, String outputKey, @Nullable String systemPrompt) {
        this(id, agent, inputKey, outputKey, systemPrompt, List.of(), ExecutionMode.SYNC);
    }

    /**
     * 创建 Agent 节点（完整参数）
     *
     * @param id            节点 ID
     * @param agent         绑定的 Agent
     * @param inputKey      输入键（从状态中获取输入）
     * @param outputKey     输出键（将输出写入状态）
     * @param systemPrompt  系统提示（可选）
     * @param toolNames     工具名称列表
     * @param executionMode 执行模式
     */
    public DefaultAgentNode(
            String id,
            Agent agent,
            String inputKey,
            String outputKey,
            @Nullable String systemPrompt,
            List<String> toolNames,
            ExecutionMode executionMode) {
        this.id = id;
        this.agent = agent;
        this.inputKey = inputKey;
        this.outputKey = outputKey;
        this.systemPrompt = systemPrompt;
        this.toolNames = toolNames;
        this.executionMode = executionMode;
    }

    @Override
    @NonNull
    public String getId() {
        return id;
    }

    @Override
    @NonNull
    public NodeResult execute(@NonNull WorkflowState state) {
        try {
            AgentRequest request = buildRequest(state);
            AgentResponse response = agent.execute(request);
            WorkflowState updatedState = processResponse(response, state);
            return NodeResult.success(updatedState);
        } catch (Exception e) {
            return NodeResult.failure(state, "Agent execution failed: " + e.getMessage());
        }
    }

    @Override
    @NonNull
    public NodeType getType() {
        return NodeType.AGENT;
    }

    @Override
    @NonNull
    public Agent getAgent() {
        return agent;
    }

    @Override
    @Nullable
    public String getSystemPrompt() {
        return systemPrompt;
    }

    @Override
    @NonNull
    public List<String> getToolNames() {
        return toolNames;
    }

    @Override
    @NonNull
    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    /**
     * 获取输入键
     *
     * @return 输入键
     */
    @NonNull
    public String getInputKey() {
        return inputKey;
    }

    /**
     * 获取输出键
     *
     * @return 输出键
     */
    @NonNull
    public String getOutputKey() {
        return outputKey;
    }

    @Override
    @NonNull
    public AgentRequest buildRequest(@NonNull WorkflowState state) {
        // 从状态中获取输入
        String userInput = state.get(inputKey);
        if (userInput == null) {
            userInput = "";
        }

        // 从状态中获取 workflowId（作为 sessionId）
        String workflowId = state.get("_workflowId");
        if (workflowId == null) {
            workflowId = "unknown-session";
        }

        // 构建上下文
        Map<String, Object> context = Map.of(
                "nodeId", id,
                "agentName", agent.getName()
        );

        return new AgentRequest(
                workflowId,
                userInput,
                context,
                List.of()
        );
    }

    @Override
    @NonNull
    public WorkflowState processResponse(@NonNull AgentResponse response, @NonNull WorkflowState state) {
        // 将输出写入状态，使用节点 ID 作为前缀
        String outputKeyWithPrefix = id + "_" + outputKey;
        WorkflowState updatedState = state.with(outputKeyWithPrefix, response.output());

        // 如果有工具调用，也写入状态
        if (!response.toolCalls().isEmpty()) {
            updatedState = updatedState.with(id + "_toolCalls", response.toolCalls());
        }

        // 写入响应状态
        updatedState = updatedState.with(id + "_status", response.status().name());

        return updatedState;
    }
}