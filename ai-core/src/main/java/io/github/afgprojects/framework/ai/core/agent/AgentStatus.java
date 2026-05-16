package io.github.afgprojects.framework.ai.core.agent;

/**
 * Agent 执行状态枚举
 *
 * <p>定义 Agent 执行过程中的各种状态：
 * <ul>
 *   <li>{@link #COMPLETED} - 执行完成</li>
 *   <li>{@link #NEEDS_INPUT} - 需要用户输入</li>
 *   <li>{@link #TOOL_CALLING} - 正在调用工具</li>
 *   <li>{@link #ERROR} - 执行出错</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public enum AgentStatus {

    /**
     * 执行完成
     */
    COMPLETED,

    /**
     * 需要用户输入
     */
    NEEDS_INPUT,

    /**
     * 正在调用工具
     */
    TOOL_CALLING,

    /**
     * 执行出错
     */
    ERROR
}
