package io.github.afgprojects.framework.ai.core.api.multiagent.human;

/**
 * 审批决策枚举
 *
 * <p>表示人机交互的决策结果。
 */
public enum HumanDecision {
    /**
     * 已批准
     */
    APPROVED,

    /**
     * 已拒绝
     */
    REJECTED,

    /**
     * 超时
     */
    TIMEOUT,

    /**
     * 已取消
     */
    CANCELLED
}
