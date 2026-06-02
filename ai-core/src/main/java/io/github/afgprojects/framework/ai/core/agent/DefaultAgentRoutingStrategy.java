package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRoutingStrategy;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 默认 Agent 路由策略
 *
 * <p>按 taskType 路由：从请求的上下文中读取 {@code taskType} 字段，
 * 匹配名称相同的已注册 Agent。如果未找到匹配，则返回第一个可用的 Agent。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultAgentRoutingStrategy implements AgentRoutingStrategy {

    @Override
    @Nullable
    public String selectAgent(@NonNull Map<String, Agent> agents, @NonNull Object request) {
        if (agents.isEmpty()) {
            return null;
        }

        // 尝试从 AgentRequest 的 context 中获取 taskType
        if (request instanceof AgentRequest agentRequest) {
            Object taskType = agentRequest.context().get("taskType");
            if (taskType != null) {
                String agentName = taskType.toString().toLowerCase();
                if (agents.containsKey(agentName)) {
                    return agentName;
                }
            }
        }

        // 默认选择第一个可用的 Agent
        return agents.keySet().stream().findFirst().orElse(null);
    }
}