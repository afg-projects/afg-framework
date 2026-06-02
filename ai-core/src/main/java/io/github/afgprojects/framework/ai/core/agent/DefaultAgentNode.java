package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.multiagent.node.AgentNode;
import io.github.afgprojects.framework.ai.core.api.workflow.engine.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 默认 Agent 节点实现
 *
 * <p>将 Agent 适配为工作流节点，在工作流中执行 Agent 任务。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultAgentNode implements AgentNode {

    private final String nodeId;
    private final Agent agent;
    private final String roleDescription;

    /**
     * 创建 Agent 节点
     *
     * @param nodeId 节点 ID
     * @param agent  绑定的 Agent
     */
    public DefaultAgentNode(@NonNull String nodeId, @NonNull Agent agent) {
        this(nodeId, agent, null);
    }

    /**
     * 创建 Agent 节点
     *
     * @param nodeId         节点 ID
     * @param agent          绑定的 Agent
     * @param roleDescription 角色描述
     */
    public DefaultAgentNode(@NonNull String nodeId, @NonNull Agent agent, @Nullable String roleDescription) {
        this.nodeId = nodeId;
        this.agent = agent;
        this.roleDescription = roleDescription;
    }

    @Override
    @NonNull
    public String getNodeId() {
        return nodeId;
    }

    @Override
    @NonNull
    public String getType() {
        return "agent";
    }

    @Override
    @NonNull
    public NodeOutput execute(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        try {
            // 从上下文中获取用户输入
            String userInput = resolveInput(context, params);

            // 构建 Agent 请求
            AgentRequest request = new AgentRequest(
                    context.getConversationId(),
                    userInput,
                    Map.of("nodeId", nodeId, "workflowId", context.getWorkflowId()),
                    java.util.List.of()
            );

            // 执行 Agent
            AgentResponse response = agent.execute(request);

            // 构建输出
            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("output", response.output());
            data.put("status", response.status().name());
            if (!response.toolCalls().isEmpty()) {
                data.put("toolCalls", response.toolCalls());
            }

            return NodeOutput.of(data, nodeId);

        } catch (Exception e) {
            log.error("Agent node execution failed: nodeId={}, error={}", nodeId, e.getMessage());
            Map<String, Object> data = Map.of("error", e.getMessage());
            return NodeOutput.of(data, nodeId);
        }
    }

    @Override
    @NonNull
    public Flux<WorkflowNode.NodeEvent> executeStream(@NonNull ExecutionContext context, @NonNull Map<String, Object> params) {
        return Flux.create(sink -> {
            try {
                NodeOutput output = execute(context, params);
                sink.next(WorkflowNode.NodeEvent.complete(output));
                sink.complete();
            } catch (Exception e) {
                sink.error(e);
            }
        });
    }

    @Override
    @NonNull
    public Agent getAgent() {
        return agent;
    }

    @Override
    @Nullable
    public String getRoleDescription() {
        return roleDescription;
    }

    private String resolveInput(ExecutionContext context, Map<String, Object> params) {
        // 优先从 params 获取
        Object input = params.get("input");
        if (input != null) {
            return input.toString();
        }

        // 从上下文变量获取
        Object userInput = context.getVariables().get("userInput");
        if (userInput != null) {
            return userInput.toString();
        }

        return "";
    }
}