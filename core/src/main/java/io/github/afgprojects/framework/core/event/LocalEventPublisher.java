package io.github.afgprojects.framework.core.event;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 本地事件发布器
 *
 * <p>基于 Spring ApplicationEventPublisher 实现的本地事件发布器。
 * 适用于单体应用或不需要分布式消息的场景。
 *
 * <p>特点：
 * <ul>
 *   <li>轻量级，无需额外中间件</li>
 *   <li>支持同步和异步发布</li>
 *   <li>事件在同一个 JVM 内传播</li>
 *   <li>支持事务性事件发布</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class LocalEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LocalEventPublisher.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final @NonNull Executor asyncExecutor;

    /**
     * 创建本地事件发布器
     *
     * @param applicationEventPublisher Spring 事件发布器
     * @param asyncExecutor 异步执行器
     */
    public LocalEventPublisher(ApplicationEventPublisher applicationEventPublisher, @NonNull Executor asyncExecutor) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public <T> void publish(@NonNull DomainEvent<T> event) {
        log.debug("Publishing local event: eventId={}, eventType={}", event.getEventId(), event.getEventType());

        try {
            // 包装为 Spring ApplicationEvent
            DomainEventWrapper<T> wrapper = new DomainEventWrapper<>(event);
            applicationEventPublisher.publishEvent(wrapper);

            log.debug("Successfully published local event: eventId={}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to publish local event: eventId={}, eventType={}",
                    event.getEventId(), event.getEventType(), e);
            throw new EventPublishException("Failed to publish local event: " + event.getEventId(), e);
        }
    }

    @Override
    public <T> CompletableFuture<Void> publishAsync(@NonNull DomainEvent<T> event) {
        return CompletableFuture.runAsync(() -> publish(event), asyncExecutor);
    }

    /**
     * 领域事件包装器
     *
     * <p>将 DomainEvent 包装为 Spring ApplicationEvent，
     * 便于使用 Spring 的事件监听机制
     *
     * @param <T> 事件载荷类型
     */
    public static class DomainEventWrapper<T> extends org.springframework.context.ApplicationEvent {

        private final DomainEvent<T> domainEvent;

        /**
         * 创建领域事件包装器
         *
         * @param domainEvent 领域事件
         */
        public DomainEventWrapper(@NonNull DomainEvent<T> domainEvent) {
            super(domainEvent);
            this.domainEvent = domainEvent;
        }

        /**
         * 获取领域事件
         *
         * @return 领域事件
         */
        @NonNull public DomainEvent<T> getDomainEvent() {
            return domainEvent;
        }

        @Override
        public String toString() {
            return "DomainEventWrapper{" + "domainEvent="
                    + JacksonUtils.toJson(domainEvent) + '}';
        }
    }
}