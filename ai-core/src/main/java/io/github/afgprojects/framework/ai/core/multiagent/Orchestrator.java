package io.github.afgprojects.framework.ai.core.multiagent;

import io.github.afgprojects.framework.ai.core.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.agent.AgentResponse;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 编排者接口
 *
 * <p>负责 Multi-Agent 系统的整体编排，包括任务分解、Agent 选择和执行调度。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface Orchestrator {

    /**
     * 获取编排者名称
     *
     * @return 名称
     */
    @NonNull String getName();

    /**
     * 编排执行请求
     *
     * <p>将请求分解为子任务，分配给合适的 Agent 执行，并汇总结果。
     *
     * @param request Agent 请求
     * @return 执行结果
     */
    @NonNull CompletableFuture<AgentResponse> orchestrate(@NonNull AgentRequest request);

    /**
     * 注册 Agent
     *
     * @param registration Agent 注册信息
     */
    void registerAgent(@NonNull AgentRegistration registration);

    /**
     * 注销 Agent
     *
     * @param agentId Agent 唯一标识
     */
    void unregisterAgent(@NonNull String agentId);

    /**
     * 获取所有已注册的 Agent
     *
     * @return Agent 注册信息列表
     */
    @NonNull List<AgentRegistration> getRegisteredAgents();

    /**
     * 根据能力选择合适的 Agent
     *
     * @param capability 能力描述
     * @return 匹配的 Agent 注册信息列表
     */
    @NonNull List<AgentRegistration> selectAgents(@NonNull String capability);
}
