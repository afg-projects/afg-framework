package io.github.afgprojects.framework.ai.core.api.multiagent;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Agent 注册信息
 *
 * <p>记录 Agent 在 Multi-Agent 系统中的注册信息，包括角色、能力和配置。
 *
 * @param agentId      Agent 唯一标识
 * @param agentName    Agent 名称
 * @param role         Agent 角色
 * @param capabilities 能力描述
 * @param config       配置参数
 * @author afg-projects
 * @since 1.0.0
 */
public record AgentRegistration(
        @NonNull String agentId,
        @NonNull String agentName,
        @NonNull AgentRole role,
        @NonNull String capabilities,
        @Nullable Map<String, Object> config
) {

    /**
     * 创建简单的 Agent 注册信息
     *
     * @param agentId   Agent 唯一标识
     * @param agentName Agent 名称
     * @param role      Agent 角色
     * @return Agent 注册信息
     */
    public static @NonNull AgentRegistration simple(
            @NonNull String agentId,
            @NonNull String agentName,
            @NonNull AgentRole role
    ) {
        return new AgentRegistration(agentId, agentName, role, "", null);
    }

    /**
     * 创建带能力的 Agent 注册信息
     *
     * @param agentId      Agent 唯一标识
     * @param agentName    Agent 名称
     * @param role         Agent 角色
     * @param capabilities 能力描述
     * @return Agent 注册信息
     */
    public static @NonNull AgentRegistration withCapabilities(
            @NonNull String agentId,
            @NonNull String agentName,
            @NonNull AgentRole role,
            @NonNull String capabilities
    ) {
        return new AgentRegistration(agentId, agentName, role, capabilities, null);
    }
}
