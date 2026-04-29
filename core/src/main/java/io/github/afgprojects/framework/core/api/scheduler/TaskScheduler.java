package io.github.afgprojects.framework.core.api.scheduler;

import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 任务调度器接口
 * 具体实现由 afg-redis 或 core 内置的本地调度器提供
 */
public interface TaskScheduler {
    @NonNull ScheduleHandle schedule(@NonNull String taskId, @NonNull Runnable task, @NonNull String cron);
    @NonNull ScheduleHandle scheduleAtFixedRate(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration period);
    @NonNull ScheduleHandle scheduleWithFixedDelay(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration delay);
    @NonNull ScheduleHandle scheduleOnce(@NonNull String taskId, @NonNull Runnable task, @NonNull Instant startTime);
    boolean cancel(@NonNull String taskId);
    boolean hasTask(@NonNull String taskId);
    @Nullable TaskStatus getTaskStatus(@NonNull String taskId);
    void pause(@NonNull String taskId);
    void resume(@NonNull String taskId);

    interface ScheduleHandle {
        @NonNull String taskId();
        @NonNull TaskStatus status();
    }
}
