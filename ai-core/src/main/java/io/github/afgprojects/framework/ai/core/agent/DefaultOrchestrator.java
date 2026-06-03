package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRegistration;
import io.github.afgprojects.framework.ai.core.api.multiagent.AgentRoutingStrategy;
import io.github.afgprojects.framework.ai.core.api.multiagent.Orchestrator;
import io.github.afgprojects.framework.ai.core.api.multiagent.communication.CommunicationBus;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认编排者实现
 *
 * <p>提供通用的 Multi-Agent 编排逻辑，包括：
 * <ul>
 *   <li>Agent 注册/注销 - 维护 Agent 注册表和实例映射</li>
 *   <li>路由选择 - 通过 {@link AgentRoutingStrategy} 选择合适的 Agent</li>
 *   <li>任务编排 - 委托 {@link AgentExecutor} 执行选中的 Agent</li>
 *   <li>能力匹配 - 根据能力描述筛选 Agent</li>
 * </ul>
 *
 * <p>路由策略通过 {@link AgentRoutingStrategy} 注入，默认使用 {@link DefaultAgentRoutingStrategy}。
 * 通信总线通过 {@link CommunicationBus} 注入，支持 Agent 间的消息传递。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultOrchestrator implements Orchestrator {

    private final AgentRoutingStrategy routingStrategy;
    private final AgentExecutor agentExecutor;
    private final CommunicationBus communicationBus;

    private final ConcurrentHashMap<String, AgentRegistration> registrations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> nameToAgentId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * 使用指定组件创建编排者
     *
     * @param routingStrategy  Agent 路由策略
     * @param agentExecutor    Agent 执行器
     * @param communicationBus 通信总线
     */
    public DefaultOrchestrator(
            @NonNull AgentRoutingStrategy routingStrategy,
            @NonNull AgentExecutor agentExecutor,
            @NonNull CommunicationBus communicationBus
    ) {
        this.routingStrategy = routingStrategy;
        this.agentExecutor = agentExecutor;
        this.communicationBus = communicationBus;
    }

    /**
     * 使用默认路由策略创建编排者
     *
     * @param agentExecutor    Agent 执行器
     * @param communicationBus 通信总线
     */
    public DefaultOrchestrator(
            @NonNull AgentExecutor agentExecutor,
            @NonNull CommunicationBus communicationBus
    ) {
        this(new DefaultAgentRoutingStrategy(), agentExecutor, communicationBus);
    }

    // ========== Orchestrator 接口实现 ==========

    @Override
    @NonNull
    public String getName() {
        return "DefaultOrchestrator";
    }

    @Override
    @NonNull
    public CompletableFuture<AgentResponse> orchestrate(@NonNull AgentRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            log.debug("Orchestrating request: sessionId={}", request.sessionId());

            // 通过路由策略选择 Agent
            String selectedAgentName = routingStrategy.selectAgent(agents, request);

            if (selectedAgentName == null) {
                log.warn("No suitable agent found for request: sessionId={}", request.sessionId());
                return AgentResponse.error("No suitable agent found for the task");
            }

            Agent agent = agents.get(selectedAgentName);
            if (agent == null) {
                log.warn("Selected agent '{}' not found in registry", selectedAgentName);
                return AgentResponse.error("Agent '" + selectedAgentName + "' not found in registry");
            }

            log.info("Routing task to agent: {}, sessionId={}", selectedAgentName, request.sessionId());

            // 通知目标 Agent 任务分配
            String agentId = nameToAgentId.get(selectedAgentName);
            if (agentId != null) {
                communicationBus.send(
                        getName(),
                        agentId,
                        io.github.afgprojects.framework.ai.core.api.multiagent.communication.AgentMessage.taskRequest(
                                getName(),
                                agentId,
                                request.userInput()
                        )
                );
            }

            // 委托 AgentExecutor 执行
            try {
                AgentResponse response = agentExecutor.execute(agent, request);
                log.debug("Agent '{}' execution completed: status={}", selectedAgentName, response.status());
                return response;
            } catch (Exception e) {
                log.error("Agent '{}' execution failed: sessionId={}", selectedAgentName, request.sessionId(), e);
                return AgentResponse.error("Execution failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public void registerAgent(@NonNull AgentRegistration registration) {
        String agentId = registration.agentId();
        registrations.put(agentId, registration);
        nameToAgentId.put(registration.agentName().toLowerCase(), agentId);
        log.info("Registered agent: id={}, name={}, role={}", agentId, registration.agentName(), registration.role());
    }

    @Override
    public void unregisterAgent(@NonNull String agentId) {
        AgentRegistration removed = registrations.remove(agentId);
        if (removed != null) {
            nameToAgentId.remove(removed.agentName().toLowerCase());
            agents.remove(removed.agentName().toLowerCase());
            log.info("Unregistered agent: id={}, name={}", agentId, removed.agentName());
        } else {
            agents.remove(agentId.toLowerCase());
            log.warn("Attempted to unregister unknown agent: id={}", agentId);
        }
    }

    @Override
    @NonNull
    public List<AgentRegistration> getRegisteredAgents() {
        return new ArrayList<>(registrations.values());
    }

    @Override
    @NonNull
    public List<AgentRegistration> selectAgents(@NonNull String capability) {
        List<AgentRegistration> matched = new ArrayList<>();

        for (AgentRegistration registration : registrations.values()) {
            String capabilities = registration.capabilities();
            if (capabilities != null && !capabilities.isEmpty()) {
                // 按逗号分隔能力描述，支持多能力匹配
                for (String cap : capabilities.split(",")) {
                    if (cap.trim().equalsIgnoreCase(capability.trim())) {
                        matched.add(registration);
                        break;
                    }
                }
            }
        }

        log.debug("Selected {} agent(s) for capability: '{}'", matched.size(), capability);
        return matched;
    }

    // ========== 扩展方法 ==========

    /**
     * 注册 Agent 实例
     *
     * <p>将 Agent 实例注册到内部映射，使其可通过路由策略被选中执行。
     * 通常与 {@link #registerAgent(AgentRegistration)} 配合使用。
     *
     * @param agent Agent 实例
     */
    public void registerAgentInstance(@NonNull Agent agent) {
        String name = agent.getName().toLowerCase();
        agents.put(name, agent);
        log.info("Registered agent instance: name={}", agent.getName());
    }

    /**
     * 注销 Agent 实例
     *
     * @param agentName Agent 名称
     */
    public void unregisterAgentInstance(@NonNull String agentName) {
        agents.remove(agentName.toLowerCase());
        log.info("Unregistered agent instance: name={}", agentName);
    }

    /**
     * 获取指定名称的 Agent 实例
     *
     * @param agentName Agent 名称
     * @return Agent 实例，可能为 null
     */
    @Nullable
    public Agent getAgent(@NonNull String agentName) {
        return agents.get(agentName.toLowerCase());
    }
}
