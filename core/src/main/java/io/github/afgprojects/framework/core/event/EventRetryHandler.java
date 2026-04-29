package io.github.afgprojects.framework.core.event;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 事件重试处理器
 *
 * <p>提供事件处理失败后的重试逻辑，支持指数退避策略。
 * 重试次数耗尽后，将事件发送到死信队列。
 *
 * <p>使用示例：
 * <pre>{@code
 * @Component
 * public class UserEventHandler {
 *     private final EventRetryHandler retryHandler;
 *
 *     @EventHandler(topic = "user.created", retryCount = 3)
 *     public void handleUserCreated(UserCreatedEvent event) {
 *         retryHandler.executeWithRetry(event, this::processUserCreated);
 *     }
 *
 *     private void processUserCreated(UserCreatedEvent event) {
 *         // 处理逻辑
 *     }
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class EventRetryHandler {

    private static final Logger log = LoggerFactory.getLogger(EventRetryHandler.class);

    private final EventProperties properties;
    private final @Nullable DomainEventPublisher deadLetterPublisher;
    private final ObjectMapper objectMapper;

    /**
     * 创建事件重试处理器
     *
     * @param properties 事件配置属性
     * @param deadLetterPublisher 死信队列发布器（可选）
     */
    public EventRetryHandler(
            @NonNull EventProperties properties,
            @Nullable DomainEventPublisher deadLetterPublisher) {
        this.properties = properties;
        this.deadLetterPublisher = deadLetterPublisher;
        this.objectMapper = JacksonUtils.getObjectMapper();
    }

    /**
     * 执行事件处理并支持重试
     *
     * @param event 领域事件
     * @param handler 事件处理器
     * @param <T> 事件载荷类型
     * @throws EventProcessingException 重试次数耗尽后仍失败时抛出
     */
    public <T> void executeWithRetry(@NonNull DomainEvent<T> event, @NonNull EventHandler<T> handler) {
        executeWithRetry(event, handler, properties.getRetry().getMaxAttempts());
    }

    /**
     * 执行事件处理并支持重试
     *
     * @param event 领域事件
     * @param handler 事件处理器
     * @param maxAttempts 最大重试次数
     * @param <T> 事件载荷类型
     * @throws EventProcessingException 重试次数耗尽后仍失败时抛出
     */
    public <T> void executeWithRetry(
            @NonNull DomainEvent<T> event,
            @NonNull EventHandler<T> handler,
            int maxAttempts) {
        if (!properties.getRetry().isEnabled() || maxAttempts <= 1) {
            // 不启用重试或重试次数为 1，直接执行
            try {
                handler.handle(event);
            } catch (Exception e) {
                throw new EventProcessingException("Event processing failed", e);
            }
            return;
        }

        int retryCount = 0;
        Exception lastException = null;

        while (retryCount <= maxAttempts) {
            try {
                handler.handle(event);
                log.debug("Event processed successfully: eventId={}, eventType={}, attempt={}",
                        event.getEventId(), event.getEventType(), retryCount + 1);
                return; // 成功处理，返回
            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount < maxAttempts) {
                    // 计算重试间隔
                    long interval = calculateRetryInterval(retryCount);
                    log.warn("Event processing failed, will retry: eventId={}, eventType={}, attempt={}, nextRetryAfter={}ms",
                            event.getEventId(), event.getEventType(), retryCount, interval, e);

                    try {
                        TimeUnit.MILLISECONDS.sleep(interval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // 保留原始处理异常作为 suppressed 异常
                        ie.addSuppressed(e);
                        throw new EventProcessingException("Event processing interrupted", ie);
                    }
                } else {
                    // 重试次数耗尽
                    log.error("Event processing failed after {} retries: eventId={}, eventType={}",
                            maxAttempts, event.getEventId(), event.getEventType(), e);
                }
            }
        }

        // 发送到死信队列
        if (properties.getDeadLetter().isEnabled() && deadLetterPublisher != null) {
            sendToDeadLetterQueue(event, lastException, maxAttempts);
        }

        throw new EventProcessingException(
                "Event processing failed after " + maxAttempts + " retries: " + event.getEventId(),
                lastException);
    }

    /**
     * 计算重试间隔
     *
     * <p>支持指数退避策略：{@code interval * multiplier^(retryCount-1)}
     *
     * @param retryCount 当前重试次数
     * @return 重试间隔（毫秒）
     */
    private long calculateRetryInterval(int retryCount) {
        EventProperties.RetryConfig retryConfig = properties.getRetry();

        if (retryConfig.getMultiplier() <= 1) {
            // 不启用指数退避，使用固定间隔
            return retryConfig.getInitialInterval();
        }

        // 指数退避
        double interval = retryConfig.getInitialInterval()
                * Math.pow(retryConfig.getMultiplier(), retryCount - 1);

        // 限制最大间隔
        return Math.min((long) interval, retryConfig.getMaxInterval());
    }

    /**
     * 发送事件到死信队列
     *
     * @param event 原始事件
     * @param exception 失败异常
     * @param maxRetryCount 最大重试次数
     * @param <T> 事件载荷类型
     */
    private <T> void sendToDeadLetterQueue(
            @NonNull DomainEvent<T> event,
            @Nullable Exception exception,
            int maxRetryCount) {
        try {
            String originalEventJson = objectMapper.writeValueAsString(event);
            String stackTrace = exception != null ? getStackTrace(exception) : null;

            DeadLetterEvent<T> deadLetterEvent = DeadLetterEvent.<T>builder()
                    .eventId("dlq-" + event.getEventId())
                    .originalEventType(event.getEventType())
                    .originalEventJson(originalEventJson)
                    .failureReason(exception != null ? exception.getMessage() : "Unknown error")
                    .stackTrace(stackTrace)
                    .retryCount(maxRetryCount)
                    .maxRetryCount(maxRetryCount)
                    .originalTopic(event.getEventType())
                    .failedAt(Instant.now())
                    .build();

            String deadLetterTopic = properties.getDeadLetter().getTopicPrefix() + event.getEventType();
            deadLetterPublisher.publish(deadLetterTopic, deadLetterEvent);

            log.info("Event sent to dead letter queue: eventId={}, topic={}",
                    event.getEventId(), deadLetterTopic);
        } catch (Exception e) {
            log.error("Failed to send event to dead letter queue: eventId={}", event.getEventId(), e);
        }
    }

    /**
     * 获取异常堆栈
     *
     * @param exception 异常
     * @return 堆栈字符串
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * 事件处理器函数接口
     *
     * @param <T> 事件载荷类型
     */
    @FunctionalInterface
    public interface EventHandler<T> {
        /**
         * 处理事件
         *
         * @param event 领域事件
         * @throws RuntimeException 处理失败时抛出
         */
        void handle(DomainEvent<T> event);
    }
}