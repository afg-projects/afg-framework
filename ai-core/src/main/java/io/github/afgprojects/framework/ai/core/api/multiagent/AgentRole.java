package io.github.afgprojects.framework.ai.core.api.multiagent;

/**
 * Agent 角色枚举
 *
 * <p>定义 Multi-Agent 系统中的角色类型：
 * <ul>
 *   <li>{@link #ORCHESTRATOR} - 编排者：负责任务分解和分配</li>
 *   <li>{@link #WORKER} - 执行者：执行具体任务</li>
 *   <li>{@link #REVIEWER} - 审核者：审核执行结果</li>
 *   <li>{@link #COORDINATOR} - 协调者：协调 Agent 间通信</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public enum AgentRole {

    /**
     * 编排者：负责任务分解和分配
     */
    ORCHESTRATOR,

    /**
     * 执行者：执行具体任务
     */
    WORKER,

    /**
     * 审核者：审核执行结果
     */
    REVIEWER,

    /**
     * 协调者：协调 Agent 间通信
     */
    COORDINATOR
}
