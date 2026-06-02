package io.github.afgprojects.framework.ai.core.api.multiagent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Agent 路由策略接口
 *
 * <p>定义如何根据请求选择合适的 Agent 执行任务。
 * 实现类可以基于任务类型、能力匹配、负载均衡等策略进行路由。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 按任务类型路由
 * public class TaskTypeRoutingStrategy implements AgentRoutingStrategy {
 *     @Override
 *     public String selectAgent(Map<String, Agent> agents, Object request) {
 *         if (request instanceof TaskRequest taskRequest) {
 *             String taskType = taskRequest.getTaskType();
 *             if (taskType != null && agents.containsKey(taskType.toLowerCase())) {
 *                 return taskType.toLowerCase();
 *             }
 *         }
 *         return null;
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@FunctionalInterface
public interface AgentRoutingStrategy {

    /**
     * 根据请求选择合适的 Agent
     *
     * @param agents  可用的 Agent 映射（名称 -> Agent）
     * @param request 执行请求
     * @return 选中的 Agent 名称，如果没有合适的返回 null
     */
    @Nullable String selectAgent(@NonNull Map<String, Agent> agents, @NonNull Object request);
}
