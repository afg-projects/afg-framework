package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 任务执行日志
 *
 * <p>记录单次任务执行的详细信息
 *
 * @param executionId   执行 ID（唯一标识一次执行）
 * @param taskId        任务 ID
 * @param taskGroup     任务分组
 * @param nodeId        执行节点 ID
 * @param startTime     开始时间
 * @param endTime       结束时间
 * @param status        执行状态
 * @param errorMessage  错误信息
 * @param errorStack    错误堆栈
 * @param retried       重试次数
 */
public record TaskExecutionLog(
    @NonNull String executionId,
    @NonNull String taskId,
    @NonNull String taskGroup,
    @NonNull String nodeId,
    @NonNull Instant startTime,
    @Nullable Instant endTime,
    @NonNull ExecutionStatus status,
    @Nullable String errorMessage,
    @Nullable String errorStack,
    int retried
) {
    /**
     * 执行状态
     */
    public enum ExecutionStatus {
        /**
         * 运行中
         */
        RUNNING,
        /**
         * 成功
         */
        SUCCESS,
        /**
         * 失败
         */
        FAILED,
        /**
         * 超时
         */
        TIMEOUT,
        /**
         * 取消
         */
        CANCELLED,
        /**
         * 跳过（未获取到锁等）
         */
        SKIPPED
    }

    /**
     * 获取执行耗时
     */
    @NonNull
    public Duration duration() {
        if (endTime == null) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.between(startTime, endTime);
    }

    /**
     * 是否成功
     */
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    /**
     * 是否失败
     */
    public boolean isFailed() {
        return status == ExecutionStatus.FAILED || status == ExecutionStatus.TIMEOUT;
    }

    /**
     * 创建执行中的日志
     */
    public static TaskExecutionLog running(@NonNull String executionId, @NonNull String taskId,
                                           @NonNull String taskGroup, @NonNull String nodeId) {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, Instant.now(),
            null, ExecutionStatus.RUNNING, null, null, 0);
    }

    /**
     * 标记成功
     */
    @NonNull
    public TaskExecutionLog markSuccess() {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            Instant.now(), ExecutionStatus.SUCCESS, null, null, retried);
    }

    /**
     * 标记失败
     */
    @NonNull
    public TaskExecutionLog markFailed(@NonNull String errorMessage, @Nullable String errorStack) {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            Instant.now(), ExecutionStatus.FAILED, errorMessage, errorStack, retried);
    }

    /**
     * 标记超时
     */
    @NonNull
    public TaskExecutionLog markTimeout() {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            Instant.now(), ExecutionStatus.TIMEOUT, "Task execution timeout", null, retried);
    }

    /**
     * 标记取消
     */
    @NonNull
    public TaskExecutionLog markCancelled() {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            Instant.now(), ExecutionStatus.CANCELLED, null, null, retried);
    }

    /**
     * 标记跳过
     */
    @NonNull
    public TaskExecutionLog markSkipped(@NonNull String reason) {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            Instant.now(), ExecutionStatus.SKIPPED, reason, null, retried);
    }

    /**
     * 增加重试计数
     */
    @NonNull
    public TaskExecutionLog incrementRetry() {
        return new TaskExecutionLog(executionId, taskId, taskGroup, nodeId, startTime,
            endTime, status, errorMessage, errorStack, retried + 1);
    }
}
