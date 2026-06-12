package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * NoOp 分布式任务调度器降级实现
 * <p>
 * 当没有 Redis 等调度后端时，提供本地降级。
 * 所有调度操作返回空句柄，查询返回 null/false。
 */
public class NoOpDistributedTaskScheduler implements DistributedTaskScheduler {

    private static final String NODE_ID = "noop-node";

    @Override
    @NonNull
    public String getCurrentNodeId() {
        return NODE_ID;
    }

    @Override
    @NonNull
    public List<String> getClusterNodes() {
        return List.of(NODE_ID);
    }

    @Override
    @NonNull
    public List<ScheduleHandle> scheduleSharded(@NonNull String taskId, @NonNull ShardedTask task,
                                                 int shardCount, @NonNull String cron) {
        return List.of();
    }

    @Override
    @NonNull
    public List<TaskExecution> getExecutionHistory(@NonNull String taskId, int limit) {
        return List.of();
    }

    @Override
    public void triggerNow(@NonNull String taskId) {
        // no-op
    }

    // ==================== TaskScheduler 接口实现 ====================

    @Override
    @NonNull
    public ScheduleHandle schedule(@NonNull String taskId, @NonNull Runnable task, @NonNull String cron) {
        return new NoOpScheduleHandle(taskId);
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleAtFixedRate(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration period) {
        return new NoOpScheduleHandle(taskId);
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleWithFixedDelay(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration delay) {
        return new NoOpScheduleHandle(taskId);
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleOnce(@NonNull String taskId, @NonNull Runnable task, @NonNull Instant startTime) {
        return new NoOpScheduleHandle(taskId);
    }

    @Override
    public boolean cancel(@NonNull String taskId) {
        return false;
    }

    @Override
    public boolean hasTask(@NonNull String taskId) {
        return false;
    }

    @Override
    @Nullable
    public TaskStatus getTaskStatus(@NonNull String taskId) {
        return null;
    }

    @Override
    public void pause(@NonNull String taskId) {
        // no-op
    }

    @Override
    public void resume(@NonNull String taskId) {
        // no-op
    }

    /**
     * NoOp 调度句柄
     */
    private record NoOpScheduleHandle(@NonNull String taskId) implements ScheduleHandle {
        @Override
        @NonNull
        public TaskStatus status() {
            return TaskStatus.CANCELLED;
        }
    }
}
