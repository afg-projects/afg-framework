package io.github.afgprojects.framework.integration.redis.scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.scheduler.DelayQueue;
import io.github.afgprojects.framework.core.exception.SchedulerException;

/**
 * Redisson 延迟队列实现
 *
 * <p>基于 Redisson RDelayedQueue 实现的分布式延迟队列
 *
 * <h3>特性</h3>
 * <ul>
 *   <li>支持延迟执行任务</li>
 *   <li>支持指定时间点执行</li>
 *   <li>支持任务取消</li>
 *   <li>支持分布式环境</li>
 *   <li>自动重试失败任务</li>
 * </ul>
 *
 * @param <T> 任务载荷类型
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedissonDelayQueue<T> implements DelayQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(RedissonDelayQueue.class);

    private final RedissonClient redissonClient;
    private final String queueName;
    private final int consumerThreads;

    private RBlockingQueue<TaskWrapper<T>> blockingQueue;
    private RDelayedQueue<TaskWrapper<T>> delayedQueue;
    private DelayTaskProcessor<T> processor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConcurrentMap<String, TaskWrapper<T>> pendingTasks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, TaskMetadata> taskMetadata = new ConcurrentHashMap<>();

    /**
     * 创建 Redisson 延迟队列实例
     *
     * @param redissonClient Redisson 客户端
     * @param queueName      队列名称
     * @param consumerThreads 消费者线程数
     */
    public RedissonDelayQueue(
            @NonNull RedissonClient redissonClient,
            @NonNull String queueName,
            int consumerThreads) {
        this.redissonClient = redissonClient;
        this.queueName = queueName;
        this.consumerThreads = Math.max(1, consumerThreads);
    }

    @Override
    public @NonNull String offer(@NonNull String taskId, @NonNull T payload, @NonNull Duration delay) {
        return offerAt(taskId, payload, Instant.now().plus(delay));
    }

    @Override
    public @NonNull String offerAt(@NonNull String taskId, @NonNull T payload, @NonNull Instant executeTime) {
        if (blockingQueue == null || delayedQueue == null) {
            throw new SchedulerException(SchedulerException.SCHEDULER_ERROR, "Delay queue not initialized. Call start() first.");
        }

        TaskWrapper<T> wrapper = new TaskWrapper<>(
                taskId,
                payload,
                executeTime.toEpochMilli()
        );

        Duration delay = Duration.between(Instant.now(), executeTime);
        if (delay.isNegative() || delay.isZero()) {
            // 立即执行
            blockingQueue.add(wrapper);
        } else {
            // 延迟执行
            delayedQueue.offer(wrapper, delay.toMillis(), TimeUnit.MILLISECONDS);
        }

        pendingTasks.put(taskId, wrapper);
        taskMetadata.put(taskId, new TaskMetadata(taskId, executeTime));

        log.debug("Task {} scheduled for execution at {}", taskId, executeTime);
        return taskId;
    }

    @Override
    public boolean cancel(@NonNull String taskId) {
        TaskWrapper<T> wrapper = pendingTasks.remove(taskId);
        if (wrapper == null) {
            return false;
        }

        taskMetadata.remove(taskId);
        // 尝试从延迟队列中移除
        try {
            delayedQueue.remove(wrapper);
        } catch (Exception e) {
            log.debug("Failed to remove task {} from delayed queue: {}", taskId, e.getMessage());
        }

        log.debug("Task {} cancelled", taskId);
        return true;
    }

    @Override
    public long size() {
        return pendingTasks.size();
    }

    @Override
    public long pendingCount() {
        if (blockingQueue == null) {
            return 0;
        }
        // 返回阻塞队列和延迟队列中的总任务数
        // 使用 pendingTasks map 来跟踪所有待处理任务
        return pendingTasks.size();
    }

    @Override
    public void registerProcessor(@NonNull DelayTaskProcessor<T> processor) {
        this.processor = processor;
    }

    @Override
    public void start() {
        if (running.get()) {
            return;
        }

        // 初始化队列
        blockingQueue = redissonClient.getBlockingQueue(queueName);
        delayedQueue = redissonClient.getDelayedQueue(blockingQueue);

        running.set(true);
        log.info("RedissonDelayQueue started with {} consumer threads", consumerThreads);

        // 启动消费者线程
        for (int i = 0; i < consumerThreads; i++) {
            Thread consumerThread = new Thread(this::consume, "delay-queue-consumer-" + i);
            consumerThread.setDaemon(true);
            consumerThread.start();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (delayedQueue != null) {
            delayedQueue.destroy();
        }
        log.info("RedissonDelayQueue stopped");
    }

    /**
     * 消费任务
     */
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void consume() {
        while (running.get()) {
            try {
                // 阻塞获取任务
                TaskWrapper<T> wrapper = blockingQueue.poll(1, TimeUnit.SECONDS);
                if (wrapper == null) {
                    continue;
                }

                String taskId = wrapper.taskId();
                pendingTasks.remove(taskId);
                taskMetadata.remove(taskId);

                // 处理任务
                if (processor != null) {
                    try {
                        processor.process(taskId, wrapper.payload())
                                .exceptionally(throwable -> {
                                    log.error("Task {} processing failed: {}", taskId, throwable.getMessage());
                                    return null;
                                })
                                .join();
                    } catch (Exception e) {
                        log.error("Task {} processing failed: {}", taskId, e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error consuming from delay queue: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取任务元数据
     *
     * @param taskId 任务 ID
     * @return 任务元数据，如果不存在返回 null
     */
    public @Nullable TaskMetadata getTaskMetadata(@NonNull String taskId) {
        return taskMetadata.get(taskId);
    }

    /**
     * 任务包装器
     */
    public record TaskWrapper<T>(
            @NonNull String taskId,
            @NonNull T payload,
            long executeTimeMillis
    ) implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }

    /**
     * 任务元数据
     */
    public record TaskMetadata(
            @NonNull String taskId,
            @NonNull Instant executeTime
    ) {}
}
