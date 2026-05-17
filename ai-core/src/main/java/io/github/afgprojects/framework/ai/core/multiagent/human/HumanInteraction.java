package io.github.afgprojects.framework.ai.core.multiagent.human;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 人机交互接口
 *
 * <p>定义人机交互的标准接口，支持审批、输入等交互类型。
 */
public interface HumanInteraction {

    /**
     * 请求审批
     *
     * @param interactionId 交互ID
     * @param prompt        提示信息
     * @param content       待审批内容
     * @param timeout       超时时间
     * @return 审批决策的异步结果
     */
    @NonNull
    CompletableFuture<HumanDecision> requestApproval(
            @NonNull String interactionId,
            @NonNull String prompt,
            @Nullable Object content,
            @NonNull Duration timeout
    );

    /**
     * 请求输入
     *
     * @param interactionId 交互ID
     * @param prompt        提示信息
     * @param schema        输入模式（可选）
     * @param timeout       超时时间
     * @return 用户输入的异步结果
     */
    @NonNull
    CompletableFuture<Object> requestInput(
            @NonNull String interactionId,
            @NonNull String prompt,
            @Nullable Object schema,
            @NonNull Duration timeout
    );

    /**
     * 提交决策
     *
     * @param interactionId 交互ID
     * @param decision      决策结果
     */
    void submitDecision(@NonNull String interactionId, @NonNull HumanDecision decision);

    /**
     * 获取待处理交互列表
     *
     * @return 待处理的交互列表
     */
    @NonNull
    List<?> getPendingInteractions();
}
