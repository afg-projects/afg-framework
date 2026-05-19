package io.github.afgprojects.framework.core.api.event;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * 消息事件（消息队列风格）
 *
 * <p>此 record 用于消息队列事件，与 {@link io.github.afgprojects.framework.core.event.DomainEvent} 不同：
 * <ul>
 *   <li>此 record - 简单数据载体，用于 Kafka/RabbitMQ 等消息队列</li>
 *   <li>{@code core.event.DomainEvent} - DDD 风格接口，用于本地事件总线</li>
 * </ul>
 */
public record MessageEvent<T>(
    @NonNull String eventId,
    @NonNull String topic,
    @NonNull T payload,
    @NonNull Instant occurredAt,
    @NonNull String source
) {
    public MessageEvent(String topic, T payload) {
        this(UUID.randomUUID().toString(), topic, payload, Instant.now(), "afg-core");
    }

    public MessageEvent(String topic, T payload, String source) {
        this(UUID.randomUUID().toString(), topic, payload, Instant.now(), source);
    }
}
