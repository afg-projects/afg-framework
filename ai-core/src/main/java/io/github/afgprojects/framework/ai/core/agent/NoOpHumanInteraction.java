package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanDecision;
import io.github.afgprojects.framework.ai.core.api.multiagent.human.HumanInteraction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 空操作人机交互实现。
 *
 * <p>不支持人机交互的空实现，用于不需要人工干预的自动化场景。
 *
 * <p>所有交互请求均返回超时决策：
 * <ul>
 *   <li>{@link #requestApproval} - 返回 {@link HumanDecision#TIMEOUT}</li>
 *   <li>{@link #requestInput} - 返回 null</li>
 *   <li>{@link #submitDecision} - 空操作</li>
 *   <li>{@link #getPendingInteractions} - 返回空列表</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class NoOpHumanInteraction implements HumanInteraction {

    @Override
    public @NonNull CompletableFuture<HumanDecision> requestApproval(
            @NonNull String interactionId,
            @NonNull String prompt,
            @Nullable Object content,
            @NonNull Duration timeout) {
        return CompletableFuture.completedFuture(HumanDecision.TIMEOUT);
    }

    @Override
    public @NonNull CompletableFuture<Object> requestInput(
            @NonNull String interactionId,
            @NonNull String prompt,
            @Nullable Object schema,
            @NonNull Duration timeout) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void submitDecision(@NonNull String interactionId, @NonNull HumanDecision decision) {
        // no-op
    }

    @Override
    public @NonNull List<?> getPendingInteractions() {
        return Collections.emptyList();
    }
}
