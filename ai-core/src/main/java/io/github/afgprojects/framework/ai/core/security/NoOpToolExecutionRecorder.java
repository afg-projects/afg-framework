package io.github.afgprojects.framework.ai.core.security;

import io.github.afgprojects.framework.ai.core.api.tool.ToolContext;
import io.github.afgprojects.framework.ai.core.api.tool.ToolExecutionRecorder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;

/**
 * 空操作工具执行记录器
 *
 * <p>不执行任何记录操作的空实现，用于不需要记录工具执行过程的场景。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class NoOpToolExecutionRecorder implements ToolExecutionRecorder {

    @Override
    public @NonNull String recordStart(
            @NonNull String toolName,
            @NonNull Map<String, Object> arguments,
            @NonNull ToolContext context) {
        return "noop-" + System.nanoTime();
    }

    @Override
    public void recordSuccess(
            @NonNull String executionId,
            @Nullable Object output,
            @NonNull Duration duration) {
        // no-op
    }

    @Override
    public void recordFailure(
            @NonNull String executionId,
            @NonNull String error,
            @NonNull Duration duration) {
        // no-op
    }
}
