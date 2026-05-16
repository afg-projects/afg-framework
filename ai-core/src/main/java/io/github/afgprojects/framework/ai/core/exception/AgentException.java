package io.github.afgprojects.framework.ai.core.exception;

import org.jspecify.annotations.NonNull;

/**
 * Agent 相关异常
 *
 * <p>处理 Agent 执行过程中的异常情况。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class AgentException extends AiException {

    private final @NonNull String agentName;

    /**
     * 创建 Agent 异常
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param agentName Agent 名称
     */
    public AgentException(@NonNull String message, @NonNull String errorCode, @NonNull String agentName) {
        super(message, errorCode);
        this.agentName = agentName;
    }

    /**
     * 创建 Agent 异常（带原因）
     *
     * @param message   异常消息
     * @param errorCode 错误代码
     * @param agentName Agent 名称
     * @param cause     原因
     */
    public AgentException(@NonNull String message, @NonNull String errorCode, @NonNull String agentName, Throwable cause) {
        super(message, errorCode, cause);
        this.agentName = agentName;
    }

    /**
     * 获取 Agent 名称
     *
     * @return Agent 名称
     */
    public @NonNull String getAgentName() {
        return agentName;
    }

    /**
     * 创建迭代次数超限异常
     */
    public static @NonNull AgentException iterationExceeded(@NonNull String agentName, int maxIterations) {
        return new AgentException(
                "Agent " + agentName + " exceeded maximum iterations: " + maxIterations,
                ErrorCodes.AGENT_ITERATION_EXCEEDED,
                agentName
        );
    }

    /**
     * 创建执行失败异常
     */
    public static @NonNull AgentException executionFailed(@NonNull String agentName, Throwable cause) {
        return new AgentException(
                "Agent " + agentName + " execution failed",
                ErrorCodes.AGENT_EXECUTION_FAILED,
                agentName,
                cause
        );
    }

    /**
     * 创建超时异常
     */
    public static @NonNull AgentException timeout(@NonNull String agentName, long timeoutMs) {
        return new AgentException(
                "Agent " + agentName + " timed out after " + timeoutMs + "ms",
                ErrorCodes.AGENT_TIMEOUT,
                agentName
        );
    }
}