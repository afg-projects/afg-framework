package io.github.afgprojects.framework.ai.core.multiagent.node;

import io.github.afgprojects.framework.ai.core.multiagent.graph.Node;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 人机交互节点接口
 *
 * <p>定义人机交互节点的标准接口，支持审批、输入、审核等交互类型。
 */
public interface HumanNode extends Node {

    /**
     * 获取交互类型
     *
     * @return 交互类型
     */
    @NonNull
    InteractionType getInteractionType();

    /**
     * 获取提示信息
     *
     * @return 提示信息
     */
    @NonNull
    String getPrompt();

    /**
     * 获取内容来源 key
     *
     * @return 内容来源 key，可能为 null
     */
    @Nullable
    String getContentKey();

    /**
     * 获取结果写入 key
     *
     * @return 结果写入 key
     */
    @NonNull
    String getResultKey();

    /**
     * 获取超时配置
     *
     * @return 超时时间
     */
    @NonNull
    Duration getTimeout();

    /**
     * 获取超时行为
     *
     * @return 超时行为
     */
    @NonNull
    TimeoutAction getTimeoutAction();

    /**
     * 交互类型枚举
     */
    enum InteractionType {
        /**
         * 审批
         */
        APPROVE,

        /**
         * 输入
         */
        INPUT,

        /**
         * 审核
         */
        REVIEW
    }

    /**
     * 超时行为枚举
     */
    enum TimeoutAction {
        /**
         * 超时自动批准
         */
        APPROVE,

        /**
         * 超时自动拒绝
         */
        REJECT,

        /**
         * 超时重试
         */
        RETRY
    }
}
