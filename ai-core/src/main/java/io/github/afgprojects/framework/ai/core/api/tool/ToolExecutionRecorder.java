package io.github.afgprojects.framework.ai.core.api.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;

/**
 * 工具执行记录器接口
 *
 * <p>记录 AI 工具的执行过程，包括执行开始、成功和失败事件。
 * 与 {@link ToolAuditLogger} 不同，本接口专注于工具执行的生命周期追踪，
 * 而非安全审计。
 *
 * <p>使用示例：
 * <pre>{@code
 * String executionId = recorder.recordStart("query_users", arguments, context);
 * try {
 *     Object result = tool.execute(input);
 *     recorder.recordSuccess(executionId, result, duration);
 * } catch (Exception e) {
 *     recorder.recordFailure(executionId, e.getMessage(), duration);
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface ToolExecutionRecorder {

    /**
     * 记录工具执行开始
     *
     * @param toolName  工具名称
     * @param arguments 工具参数
     * @param context   工具上下文
     * @return 执行记录 ID
     */
    @NonNull
    String recordStart(
        @NonNull String toolName,
        @NonNull Map<String, Object> arguments,
        @NonNull ToolContext context
    );

    /**
     * 记录工具执行成功
     *
     * @param executionId 执行记录 ID
     * @param output      工具输出
     * @param duration    执行时长
     */
    void recordSuccess(
        @NonNull String executionId,
        @Nullable Object output,
        @NonNull Duration duration
    );

    /**
     * 记录工具执行失败
     *
     * @param executionId 执行记录 ID
     * @param error       错误信息
     * @param duration    执行时长
     */
    void recordFailure(
        @NonNull String executionId,
        @NonNull String error,
        @NonNull Duration duration
    );
}
