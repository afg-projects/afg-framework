package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NonNull;

/**
 * NoOp 任务执行日志存储降级实现
 * <p>
 * 当没有 Redis 等日志存储后端时，提供本地降级。
 * 所有写入操作静默丢弃，查询返回空/null/0。
 */
public class NoOpTaskExecutionLogStorage implements TaskExecutionLogStorage {

    @Override
    public void save(@NonNull TaskExecutionLog log) {
        // no-op: 静默丢弃
    }

    @Override
    public void update(@NonNull TaskExecutionLog log) {
        // no-op: 静默丢弃
    }

    @Override
    @NonNull
    public Optional<TaskExecutionLog> findByExecutionId(@NonNull String executionId) {
        return Optional.empty();
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskId(@NonNull String taskId, int limit) {
        return List.of();
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTaskGroup(@NonNull String taskGroup, int limit) {
        return List.of();
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findByTimeRange(@NonNull String taskId, @NonNull Instant from, @NonNull Instant to) {
        return List.of();
    }

    @Override
    @NonNull
    public List<TaskExecutionLog> findFailedExecutions(@NonNull String taskId, int limit) {
        return List.of();
    }

    @Override
    public long countByTaskId(@NonNull String taskId) {
        return 0;
    }

    @Override
    public long countSuccessByTaskId(@NonNull String taskId) {
        return 0;
    }

    @Override
    public long countFailedByTaskId(@NonNull String taskId) {
        return 0;
    }

    @Override
    public double getAverageExecutionTime(@NonNull String taskId) {
        return 0.0;
    }

    @Override
    public long deleteBefore(@NonNull Instant before) {
        return 0;
    }

    @Override
    public void deleteByTaskId(@NonNull String taskId) {
        // no-op
    }
}
