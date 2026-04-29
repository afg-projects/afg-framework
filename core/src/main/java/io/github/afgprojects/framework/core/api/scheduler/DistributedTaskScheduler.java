package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 分布式任务调度器接口
 * 具体实现由 afg-redis 提供
 */
public interface DistributedTaskScheduler extends TaskScheduler {
    @NonNull String getCurrentNodeId();
    @NonNull List<String> getClusterNodes();
    @NonNull List<ScheduleHandle> scheduleSharded(@NonNull String taskId, @NonNull ShardedTask task, int shardCount, @NonNull String cron);
    @NonNull List<TaskExecution> getExecutionHistory(@NonNull String taskId, int limit);
    void triggerNow(@NonNull String taskId);

    @FunctionalInterface
    interface ShardedTask {
        void execute(int shardIndex, int shardTotal);
    }

    record TaskExecution(
        String taskId,
        String nodeId,
        Instant startTime,
        Instant endTime,
        TaskStatus status,
        @Nullable String errorMessage
    ) {}
}
