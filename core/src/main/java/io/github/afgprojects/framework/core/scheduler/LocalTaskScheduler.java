package io.github.afgprojects.framework.core.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.api.scheduler.TaskScheduler;
import io.github.afgprojects.framework.core.api.scheduler.TaskStatus;
import io.github.afgprojects.framework.core.exception.SchedulerException;

/**
 * 本地任务调度器
 *
 * <p>基于 Java ScheduledExecutorService 的本地任务调度器实现
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>支持 Cron 表达式（通过 CronUtils 解析）</li>
 *   <li>支持固定速率调度</li>
 *   <li>支持固定延迟调度</li>
 *   <li>支持一次性任务</li>
 *   <li>任务执行监控</li>
 *   <li>执行日志记录</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class LocalTaskScheduler implements TaskScheduler {

    private static final Logger log = LoggerFactory.getLogger(LocalTaskScheduler.class);

    private final ScheduledExecutorService executorService;
    private final TaskExecutionMetrics metrics;
    private final TaskExecutionLogStorage logStorage;
    private final SchedulerProperties properties;
    private final String nodeId;

    private final ConcurrentMap<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskDefinition> taskDefinitions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ScheduleHandleImpl> handles = new ConcurrentHashMap<>();

    /**
     * 创建本地任务调度器
     *
     * @param properties 调度器配置
     * @param metrics    执行监控
     * @param logStorage 日志存储
     */
    public LocalTaskScheduler(@NonNull SchedulerProperties properties,
                              @NonNull TaskExecutionMetrics metrics,
                              @NonNull TaskExecutionLogStorage logStorage) {
        this.properties = properties;
        this.metrics = metrics;
        this.logStorage = logStorage;
        this.nodeId = UUID.randomUUID().toString();

        // 创建调度线程池
        this.executorService = Executors.newScheduledThreadPool(
            properties.getThreadPoolSize(),
            r -> {
                Thread thread = new Thread(r, "afg-scheduler-" + nodeId);
                thread.setDaemon(true);
                return thread;
            }
        );

        log.info("LocalTaskScheduler initialized with nodeId={}, threadPoolSize={}",
            nodeId, properties.getThreadPoolSize());
    }

    @Override
    @NonNull
    public ScheduleHandle schedule(@NonNull String taskId, @NonNull Runnable task, @NonNull String cron) {
        validateTaskId(taskId);

        try {
            // 解析 Cron 表达式
            Duration initialDelay = CronUtils.getNextExecutionDelay(cron);
            Duration period = CronUtils.getPeriod(cron);

            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                new MonitoredRunnable(taskId, "scheduled", task),
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS
            );

            scheduledTasks.put(taskId, future);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));

            log.info("Task {} scheduled with cron: {}", taskId, cron);
            return getHandle(taskId);

        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleAtFixedRate(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration period) {
        validateTaskId(taskId);

        try {
            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(
                new MonitoredRunnable(taskId, "scheduled", task),
                0,
                period.toMillis(),
                TimeUnit.MILLISECONDS
            );

            scheduledTasks.put(taskId, future);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));

            log.info("Task {} scheduled at fixed rate: {}ms", taskId, period.toMillis());
            return getHandle(taskId);

        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleWithFixedDelay(@NonNull String taskId, @NonNull Runnable task, @NonNull Duration delay) {
        validateTaskId(taskId);

        try {
            ScheduledFuture<?> future = executorService.scheduleWithFixedDelay(
                new MonitoredRunnable(taskId, "scheduled", task),
                0,
                delay.toMillis(),
                TimeUnit.MILLISECONDS
            );

            scheduledTasks.put(taskId, future);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));

            log.info("Task {} scheduled with fixed delay: {}ms", taskId, delay.toMillis());
            return getHandle(taskId);

        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Failed to schedule task " + taskId, e);
        }
    }

    @Override
    @NonNull
    public ScheduleHandle scheduleOnce(@NonNull String taskId, @NonNull Runnable task, @NonNull Instant startTime) {
        validateTaskId(taskId);

        try {
            long delay = Duration.between(Instant.now(), startTime).toMillis();

            ScheduledFuture<?> future = executorService.schedule(
                new MonitoredRunnable(taskId, "once", task),
                delay > 0 ? delay : 0,
                TimeUnit.MILLISECONDS
            );

            scheduledTasks.put(taskId, future);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));

            log.info("Task {} scheduled once at {}", taskId, startTime);
            return getHandle(taskId);

        } catch (Exception e) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Failed to schedule task " + taskId, e);
        }
    }

    /**
     * 根据任务定义调度任务
     *
     * @param definition 任务定义
     * @param task       任务执行逻辑
     * @return 调度句柄
     */
    @NonNull
    public ScheduleHandle schedule(@NonNull TaskDefinition definition, @NonNull Runnable task) {
        String taskId = definition.taskId();
        taskDefinitions.put(taskId, definition);

        if (!definition.enabled()) {
            log.info("Task {} is disabled, not scheduling", taskId);
            return new ScheduleHandleImpl(taskId, TaskStatus.PAUSED);
        }

        if (definition.isCronTask()) {
            return schedule(taskId, task, definition.cron());
        } else if (definition.isFixedRateTask()) {
            return scheduleAtFixedRate(taskId, task, Duration.ofMillis(definition.fixedRate()));
        } else if (definition.isFixedDelayTask()) {
            return scheduleWithFixedDelay(taskId, task, Duration.ofMillis(definition.fixedDelay()));
        } else {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR,
                "Invalid task definition for " + taskId);
        }
    }

    @Override
    public boolean cancel(@NonNull String taskId) {
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
            taskDefinitions.remove(taskId);
            handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.CANCELLED));
            log.info("Task {} cancelled", taskId);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasTask(@NonNull String taskId) {
        return scheduledTasks.containsKey(taskId);
    }

    @Override
    @Nullable
    public TaskStatus getTaskStatus(@NonNull String taskId) {
        ScheduleHandle handle = handles.get(taskId);
        return handle != null ? handle.status() : null;
    }

    @Override
    public void pause(@NonNull String taskId) {
        ScheduledFuture<?> future = scheduledTasks.get(taskId);
        if (future == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND, "Task not found: " + taskId);
        }

        future.cancel(false);
        handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.PAUSED));
        log.info("Task {} paused", taskId);
    }

    @Override
    public void resume(@NonNull String taskId) {
        TaskDefinition definition = taskDefinitions.get(taskId);
        if (definition == null) {
            throw new SchedulerException(SchedulerException.JOB_NOT_FOUND,
                "Task definition not found: " + taskId);
        }

        // 需要重新调度，但 Runnable 已丢失，需要外部重新注册
        handles.put(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));
        log.info("Task {} resumed (note: re-scheduling required with Runnable)", taskId);
    }

    /**
     * 关闭调度器
     */
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("LocalTaskScheduler shutdown completed");
    }

    private void validateTaskId(@NonNull String taskId) {
        if (scheduledTasks.containsKey(taskId)) {
            throw new SchedulerException(SchedulerException.JOB_ALREADY_EXISTS,
                "Task already exists: " + taskId);
        }
    }

    private ScheduleHandle getHandle(@NonNull String taskId) {
        return handles.getOrDefault(taskId, new ScheduleHandleImpl(taskId, TaskStatus.SCHEDULED));
    }

    /**
     * 带监控的 Runnable 包装器
     */
    private class MonitoredRunnable implements Runnable {
        private final String taskId;
        private final String taskGroup;
        private final Runnable delegate;

        MonitoredRunnable(String taskId, String taskGroup, Runnable delegate) {
            this.taskId = taskId;
            this.taskGroup = taskGroup;
            this.delegate = delegate;
        }

        @Override
        public void run() {
            String executionId = null;
            TaskExecutionLog executionLog = null;

            try {
                executionId = metrics.recordStart(taskId, taskGroup);
                executionLog = TaskExecutionLog.running(executionId, taskId, taskGroup, nodeId);

                // 执行任务
                delegate.run();

                // 记录成功
                executionLog = executionLog.markSuccess();
                if (properties.getLogStorage().isLogSuccess()) {
                    logStorage.save(executionLog);
                }
                metrics.recordSuccess(executionId, taskId, taskGroup);

            } catch (Exception e) {
                // 记录失败
                String errorMessage = e.getMessage();
                String errorStack = properties.getLogStorage().isLogErrorStack() ?
                    getStackTrace(e) : null;

                if (executionLog != null) {
                    executionLog = executionLog.markFailed(errorMessage, errorStack);
                    logStorage.save(executionLog);
                }
                metrics.recordFailure(executionId, taskId, taskGroup, e.getClass().getSimpleName());

                log.error("Task {} execution failed: {}", taskId, errorMessage, e);

                // 检查重试
                TaskDefinition definition = taskDefinitions.get(taskId);
                if (definition != null && definition.maxRetries() > 0) {
                    scheduleRetry(taskId, definition);
                }
            }
        }

        private void scheduleRetry(String taskId, TaskDefinition definition) {
            log.info("Scheduling retry for task {} in {}ms", taskId, definition.retryDelay());
            executorService.schedule(
                new RetryRunnable(taskId, taskGroup, delegate, definition, 1),
                definition.retryDelay(),
                TimeUnit.MILLISECONDS
            );
        }

        private String getStackTrace(Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }
    }

    /**
     * 重试执行 Runnable
     */
    private class RetryRunnable implements Runnable {
        private final String taskId;
        private final String taskGroup;
        private final Runnable delegate;
        private final TaskDefinition definition;
        private final int attempt;

        RetryRunnable(String taskId, String taskGroup, Runnable delegate,
                     TaskDefinition definition, int attempt) {
            this.taskId = taskId;
            this.taskGroup = taskGroup;
            this.delegate = delegate;
            this.definition = definition;
            this.attempt = attempt;
        }

        @Override
        public void run() {
            String executionId = null;
            TaskExecutionLog executionLog = null;

            try {
                executionId = metrics.recordStart(taskId, taskGroup);
                metrics.recordRetry(taskId, taskGroup, attempt);

                executionLog = TaskExecutionLog.running(executionId, taskId, taskGroup, nodeId)
                    .incrementRetry();

                delegate.run();

                executionLog = executionLog.markSuccess();
                logStorage.save(executionLog);
                metrics.recordSuccess(executionId, taskId, taskGroup);

            } catch (Exception e) {
                String errorMessage = e.getMessage();
                String errorStack = properties.getLogStorage().isLogErrorStack() ?
                    getStackTrace(e) : null;

                if (executionLog != null) {
                    executionLog = executionLog.markFailed(errorMessage, errorStack);
                    logStorage.save(executionLog);
                }
                metrics.recordFailure(executionId, taskId, taskGroup, e.getClass().getSimpleName());

                // 继续重试
                if (attempt < definition.maxRetries()) {
                    long delay = (long) (definition.retryDelay() *
                        Math.pow(properties.getRetryMultiplier(), attempt));
                    log.info("Scheduling retry {} for task {} in {}ms", attempt + 1, taskId, delay);
                    executorService.schedule(
                        new RetryRunnable(taskId, taskGroup, delegate, definition, attempt + 1),
                        delay,
                        TimeUnit.MILLISECONDS
                    );
                } else {
                    log.error("Task {} failed after {} retries", taskId, definition.maxRetries());
                }
            }
        }

        private String getStackTrace(Exception e) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        }
    }

    /**
     * 调度句柄实现
     */
    private record ScheduleHandleImpl(@NonNull String taskId, @NonNull TaskStatus status)
        implements ScheduleHandle {}
}