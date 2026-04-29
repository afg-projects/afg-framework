package io.github.afgprojects.framework.integration.redis.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RScheduledExecutorService;
import org.redisson.api.RedissonClient;
import org.redisson.api.CronSchedule;
import org.redisson.api.WorkerOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.scheduler.DistributedTaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.exception.SchedulerException;

/**
 * Redisson 分布式任务调度器实现
 *
 * <p>基于 Redisson RScheduledExecutorService 实现的分布式任务调度器
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>支持 Cron 表达式调度</li>
 *   <li>支持固定速率调度</li>
 *   <li>支持一次性任务调度</li>
 *   <li>支持分片任务</li>
 *   <li>支持集群节点管理</li>
 *   <li>支持执行历史记录</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedissonTaskScheduler implements DistributedTaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(RedissonTaskScheduler.class);

    private static final String EXECUTION_HISTORY_PREFIX = "afg:scheduler:history:";
    private static final String TASK_STATUS_PREFIX = "afg:scheduler:status:";
    private static final int MAX_HISTORY_SIZE = 100;

    private final RedissonClient redissonClient;
    private final RScheduledExecutorService executorService;
    private final String nodeId;
    private final @Nullable Duration taskTimeout;

    private final ConcurrentMap<String, ScheduleHandle> handles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Runnable> taskRegistry = new ConcurrentHashMap<>();
    private final RMap<String, String> taskStatusMap;

    /**
     * 创建 Redisson 任务调度器实例
     *
     * @param redissonClient Redisson 客户端
     * @param executorName   执行器名称
     * @param workerCount    工作线程数（用于日志）
     * @param taskTimeout    任务超时时间
     */
    public RedissonTaskScheduler(
            @NonNull RedissonClient redissonClient,
            @NonNull String executorName,
            int workerCount,
            @Nullable Duration taskTimeout) {
        this.redissonClient = redissonClient;
        this.nodeId = UUID.randomUUID().toString();
        this.taskTimeout = taskTimeout;

        // 创建执行器服务
        WorkerOptions workerOptions = WorkerOptions.defaults()
                .workers(workerCount);

        if (taskTimeout != null) {
            workerOptions.taskTimeout(taskTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        this.executorService = redissonClient.getExecutorService(executorName);
        this.taskStatusMap = redissonClient.getMap(TASK_STATUS_PREFIX + "map");

        log.info("RedissonTaskScheduler initialized with nodeId={}, workers={}", nodeId, workerCount);
    }

    @Override
    public @NonNull ScheduleHandle schedule(@NonNull String taskId, @NonNull Runnable task, @NonNull String cron) {
        validateTaskId(taskId);

        try {
            taskRegistry.put(taskId, task);
            executorService.schedule(
                    taskId,
                    new DistributedRunnable(taskId, task),
                    CronSchedule.of(cron)
            );

            ScheduleHandle handle = new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED);
            handles.put(taskId, handle);
            updateTaskStatus(taskId, TaskStatus.SCHEDULED);

            log.info("Task {} scheduled with cron: {}", taskId, cron);
            return handle;
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    public @NonNull ScheduleHandle scheduleAtFixedRate(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration period) {
        validateTaskId(taskId);

        try {
            taskRegistry.put(taskId, task);
            executorService.scheduleAtFixedRate(
                    new DistributedRunnable(taskId, task),
                    0,
                    period.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            ScheduleHandle handle = new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED);
            handles.put(taskId, handle);
            updateTaskStatus(taskId, TaskStatus.SCHEDULED);

            log.info("Task {} scheduled at fixed rate: {}ms", taskId, period.toMillis());
            return handle;
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    public @NonNull ScheduleHandle scheduleWithFixedDelay(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration delay) {
        validateTaskId(taskId);

        try {
            taskRegistry.put(taskId, task);
            executorService.scheduleWithFixedDelay(
                    new DistributedRunnable(taskId, task),
                    0,
                    delay.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            ScheduleHandle handle = new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED);
            handles.put(taskId, handle);
            updateTaskStatus(taskId, TaskStatus.SCHEDULED);

            log.info("Task {} scheduled with fixed delay: {}ms", taskId, delay.toMillis());
            return handle;
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    public @NonNull ScheduleHandle scheduleOnce(@NonNull String taskId, @NonNull Runnable task, @NonNull Instant startTime) {
        validateTaskId(taskId);

        try {
            taskRegistry.put(taskId, task);
            long delay = Duration.between(Instant.now(), startTime).toMillis();

            if (delay > 0) {
                executorService.schedule(
                        new DistributedRunnable(taskId, task),
                        delay,
                        TimeUnit.MILLISECONDS
                );
            } else {
                executorService.submit(new DistributedRunnable(taskId, task));
            }

            ScheduleHandle handle = new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED);
            handles.put(taskId, handle);
            updateTaskStatus(taskId, TaskStatus.SCHEDULED);

            log.info("Task {} scheduled once at {}", taskId, startTime);
            return handle;
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    public boolean cancel(@NonNull String taskId) {
        try {
            executorService.cancelTask(taskId);
            handles.remove(taskId);
            taskRegistry.remove(taskId);
            updateTaskStatus(taskId, TaskStatus.CANCELLED);

            log.info("Task {} cancelled", taskId);
            return true;
        } catch (Exception e) {
            log.error("Failed to cancel task {}: {}", taskId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasTask(@NonNull String taskId) {
        return handles.containsKey(taskId);
    }

    @Override
    public @Nullable TaskStatus getTaskStatus(@NonNull String taskId) {
        ScheduleHandle handle = handles.get(taskId);
        if (handle != null) {
            return handle.status();
        }

        // 尝试从 Redis 获取状态
        String statusStr = taskStatusMap.get(taskId);
        if (statusStr != null) {
            try {
                return TaskStatus.valueOf(statusStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void pause(@NonNull String taskId) {
        // Redisson 不直接支持暂停，通过取消实现
        ScheduleHandle handle = handles.get(taskId);
        if (handle == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND, "Task not found: " + taskId);
        }

        try {
            executorService.cancelTask(taskId);
            updateTaskStatus(taskId, TaskStatus.PAUSED);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.PAUSED));

            log.info("Task {} paused", taskId);
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Failed to pause task " + taskId, e);
        }
    }

    @Override
    public void resume(@NonNull String taskId) {
        ScheduleHandle handle = handles.get(taskId);
        if (handle == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND, "Task not found: " + taskId);
        }

        Runnable task = taskRegistry.get(taskId);
        if (task == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND, "Task not found in registry: " + taskId);
        }

        // 注意：恢复需要重新调度，但当前实现不保存调度参数
        // 这里只是更新状态，实际使用需要重新调用 schedule 方法
        updateTaskStatus(taskId, TaskStatus.SCHEDULED);
        handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));

        log.info("Task {} resumed (note: re-scheduling required)", taskId);
    }

    @Override
    public @NonNull String getCurrentNodeId() {
        return nodeId;
    }

    @Override
    public @NonNull List<String> getClusterNodes() {
        // Redisson RScheduledExecutorService 不直接提供节点列表
        // 返回当前节点
        return Collections.singletonList(nodeId);
    }

    @Override
    public @NonNull List<ScheduleHandle> scheduleSharded(
            @NonNull String taskId,
            @NonNull ShardedTask task,
            int shardCount,
            @NonNull String cron) {

        if (shardCount <= 0) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Shard count must be positive");
        }

        List<ScheduleHandle> result = new ArrayList<>();
        for (int i = 0; i < shardCount; i++) {
            String shardTaskId = taskId + ":shard:" + i;
            final int shardIndex = i;

            ScheduleHandle handle = schedule(
                    shardTaskId,
                    () -> task.execute(shardIndex, shardCount),
                    cron
            );
            result.add(handle);
        }

        log.info("Task {} scheduled with {} shards", taskId, shardCount);
        return result;
    }

    @Override
    public @NonNull List<TaskExecution> getExecutionHistory(@NonNull String taskId, int limit) {
        try {
            RMap<String, TaskExecution> historyMap = redissonClient.getMap(EXECUTION_HISTORY_PREFIX + taskId);

            List<TaskExecution> history = new ArrayList<>();
            for (var entry : historyMap.entrySet()) {
                history.add(entry.getValue());
                if (history.size() >= limit) {
                    break;
                }
            }

            return history;
        } catch (Exception e) {
            log.error("Failed to get execution history for task {}: {}", taskId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void triggerNow(@NonNull String taskId) {
        Runnable task = taskRegistry.get(taskId);
        if (task == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND, "Task not found: " + taskId);
        }

        try {
            executorService.submit(task);
            log.info("Task {} triggered immediately", taskId);
        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.JOB_EXECUTION_ERROR, "Failed to trigger task " + taskId, e);
        }
    }

    private void validateTaskId(@NonNull String taskId) {
        if (handles.containsKey(taskId)) {
            throw new SchedulerException(SchedulerException.JOB_ALREADY_EXISTS,
                    "Task already exists: " + taskId);
        }
    }

    private void updateTaskStatus(@NonNull String taskId, @NonNull TaskStatus status) {
        taskStatusMap.put(taskId, status.name());
    }

    private void recordExecution(@NonNull String taskId, @NonNull TaskExecution execution) {
        try {
            RMap<String, TaskExecution> historyMap = redissonClient.getMap(EXECUTION_HISTORY_PREFIX + taskId);
            String key = String.valueOf(System.currentTimeMillis());
            historyMap.put(key, execution);

            // 限制历史记录大小
            if (historyMap.size() > MAX_HISTORY_SIZE) {
                // 移除最旧的记录
                String oldestKey = historyMap.keySet().stream()
                        .sorted()
                        .findFirst()
                        .orElse(null);
                if (oldestKey != null) {
                    historyMap.remove(oldestKey);
                }
            }
        } catch (Exception e) {
            log.error("Failed to record execution for task {}: {}", taskId, e.getMessage());
        }
    }

    /**
     * 分布式 Runnable 包装器
     */
    private class DistributedRunnable implements Runnable, java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String taskId;
        private final Runnable delegate;

        DistributedRunnable(String taskId, Runnable delegate) {
            this.taskId = taskId;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            Instant startTime = Instant.now();
            TaskStatus status = TaskStatus.COMPLETED;
            String errorMessage = null;

            try {
                updateTaskStatus(taskId, TaskStatus.RUNNING);
                delegate.run();
            } catch (Exception e) {
                status = TaskStatus.FAILED;
                errorMessage = e.getMessage();
                log.error("Task {} execution failed: {}", taskId, e.getMessage());
            } finally {
                Instant endTime = Instant.now();
                updateTaskStatus(taskId, status);

                // 记录执行历史
                TaskExecution execution = new TaskExecution(
                        taskId,
                        nodeId,
                        startTime,
                        endTime,
                        status,
                        errorMessage
                );
                recordExecution(taskId, execution);
            }
        }
    }

    /**
     * 调度句柄实现
     */
    private record ScheduleHandleImpl(
            @NonNull String taskId,
            @NonNull TaskStatus status
    ) implements ScheduleHandle {}
}
