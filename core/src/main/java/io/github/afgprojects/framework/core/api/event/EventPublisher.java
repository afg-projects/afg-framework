package io.github.afgprojects.framework.core.api.event;

import java.util.concurrent.CompletableFuture;
import org.jspecify.annotations.NonNull;

/**
 * 事件发布接口
 * 具体实现由 afg-event-kafka、afg-event-rabbitmq 或 core 内置的本地事件发布提供
 */
public interface EventPublisher<T> {
    void publish(@NonNull DomainEvent<T> event);
    CompletableFuture<Void> publishAsync(@NonNull DomainEvent<T> event);
    default void publish(@NonNull String topic, @NonNull T payload) {
        publish(new DomainEvent<>(topic, payload));
    }
}