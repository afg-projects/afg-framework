package io.github.afgprojects.framework.ai.core.agent;

import io.github.afgprojects.framework.ai.core.api.planning.ReActExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.ReActResult;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * No-op implementation of {@link ReActExecutor} that returns failure results.
 *
 * <p>Used as a fallback when no {@link io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient}
 * is available, ensuring the agent system can still function without an LLM backend.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class NoOpReActExecutor implements ReActExecutor {

    @Override
    @NonNull
    public ReActResult execute(@NonNull String task) {
        log.debug("NoOpReActExecutor: execute task '{}' - no chat client available", task);
        return ReActResult.failure("No chat client available, ReAct execution is disabled", List.of());
    }

    @Override
    @NonNull
    public ReActResult executeWithMaxSteps(@NonNull String task, int maxSteps) {
        log.debug("NoOpReActExecutor: executeWithMaxSteps task '{}' - no chat client available", task);
        return ReActResult.failure("No chat client available, ReAct execution is disabled", List.of());
    }

    @Override
    @NonNull
    public CompletableFuture<ReActResult> executeAsync(@NonNull String task) {
        log.debug("NoOpReActExecutor: executeAsync task '{}' - no chat client available", task);
        return CompletableFuture.completedFuture(
                ReActResult.failure("No chat client available, ReAct execution is disabled", List.of()));
    }
}
