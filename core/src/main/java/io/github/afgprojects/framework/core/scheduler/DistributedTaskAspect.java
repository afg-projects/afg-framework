package io.github.afgprojects.framework.core.scheduler;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.annotation.DistributedTask;
import io.github.afgprojects.framework.core.annotation.ScheduledTask;
import io.github.afgprojects.framework.core.api.scheduler.SchedulerProperties;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLog;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionLogStorage;
import io.github.afgprojects.framework.core.api.scheduler.TaskExecutionMetrics;
import io.github.afgprojects.framework.core.lock.DistributedLock;

/**
 * 分布式定时任务切面
 *
 * <p>处理 @{@link DistributedTask} 注解，提供分布式锁保护的任务执行
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>分布式锁保护，确保同一时间只有一个节点执行任务</li>
 *   <li>任务执行监控和指标收集</li>
 *   <li>执行日志记录</li>
 *   <li>超时检测</li>
 *   <li>失败重试</li>
 * </ul>
 *
 * @since 1.0.0
 */
@Aspect
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.SignatureDeclareThrowsException"})
public class DistributedTaskAspect {

    private static final Logger log = LoggerFactory.getLogger(DistributedTaskAspect.class);

    private final DistributedLock distributedLock;
    private final TaskExecutionMetrics metrics;
    private final TaskExecutionLogStorage logStorage;
    private final SchedulerProperties properties;

    /**
     * 创建分布式任务切面
     *
     * @param distributedLock 分布锁服务
     * @param metrics         执行监控
     * @param logStorage      日志存储
     * @param properties      调度器配置
     */
    public DistributedTaskAspect(@NonNull DistributedLock distributedLock,
                                  @NonNull TaskExecutionMetrics metrics,
                                  @NonNull TaskExecutionLogStorage logStorage,
                                  @NonNull SchedulerProperties properties) {
        this.distributedLock = distributedLock;
        this.metrics = metrics;
        this.logStorage = logStorage;
        this.properties = properties;
    }

    /**
     * 处理 @DistributedTask 注解
     *
     * @param joinPoint  切点
     * @param annotation 注解实例
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(annotation)")
    public Object aroundDistributedTask(ProceedingJoinPoint joinPoint, DistributedTask annotation) throws Throwable {
        String taskId = annotation.id();
        String taskGroup = "distributed";
        String lockKey = "scheduler:task:" + taskId;
        long lockWaitTime = annotation.lockWaitTime();
        long lockLeaseTime = annotation.lockLeaseTime() > 0 ? annotation.lockLeaseTime() : -1;

        // 检查任务是否启用
        if (!annotation.enabled()) {
            log.debug("Task {} is disabled, skipping execution", taskId);
            metrics.recordSkipped(taskId, taskGroup, "disabled");
            return null;
        }

        // 尝试获取分布式锁
        boolean acquired = distributedLock.tryLock(lockKey, lockWaitTime, lockLeaseTime);
        if (!acquired) {
            log.debug("Failed to acquire lock for task {}, skipping execution", taskId);
            metrics.recordSkipped(taskId, taskGroup, "lock_not_acquired");
            return null;
        }

        String executionId = null;
        TaskExecutionLog executionLog = null;

        try {
            // 记录开始
            executionId = metrics.recordStart(taskId, taskGroup);
            executionLog = TaskExecutionLog.running(executionId, taskId, taskGroup, "local");

            // 执行任务
            Object result = executeWithRetry(joinPoint, annotation, executionId, taskId, taskGroup);

            // 记录成功
            if (executionLog != null) {
                executionLog = executionLog.markSuccess();
                logStorage.save(executionLog);
            }
            metrics.recordSuccess(executionId, taskId, taskGroup);

            return result;

        } catch (Throwable e) {
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
            throw e;

        } finally {
            // 释放锁
            distributedLock.unlock(lockKey);
        }
    }

    /**
     * 带重试的任务执行
     */
    private Object executeWithRetry(ProceedingJoinPoint joinPoint, DistributedTask annotation,
                                    String executionId, String taskId, String taskGroup) throws Throwable {
        int maxRetries = properties.getDefaultRetryAttempts();
        int attempt = 0;
        Throwable lastError = null;

        while (attempt <= maxRetries) {
            try {
                return joinPoint.proceed();

            } catch (Throwable e) {
                lastError = e;
                attempt++;

                if (attempt <= maxRetries) {
                    long delay = calculateRetryDelay(attempt);
                    log.warn("Task {} execution failed (attempt {}/{}), retrying in {}ms: {}",
                        taskId, attempt, maxRetries, delay, e.getMessage());

                    metrics.recordRetry(taskId, taskGroup, attempt);

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // 保留原始异常堆栈
                        RuntimeException ex = new RuntimeException("Retry interrupted", ie);
                        ex.addSuppressed(e);
                        throw ex;
                    }
                }
            }
        }

        // 确保 lastError 不为 null
        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("Task execution failed without capturing exception");
    }

    /**
     * 计算重试延迟（指数退避）
     */
    private long calculateRetryDelay(int attempt) {
        long baseDelay = properties.getDefaultRetryDelay().toMillis();
        double multiplier = properties.getRetryMultiplier();
        return (long) (baseDelay * Math.pow(multiplier, attempt - 1));
    }

    /**
     * 获取异常堆栈字符串
     */
    private String getStackTrace(Throwable e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
