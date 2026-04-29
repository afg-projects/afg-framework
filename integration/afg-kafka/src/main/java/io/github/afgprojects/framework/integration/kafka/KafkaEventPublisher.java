package io.github.afgprojects.framework.integration.kafka;

import io.github.afgprojects.framework.core.api.event.DomainEvent;
import io.github.afgprojects.framework.core.api.event.EventPublisher;
import io.github.afgprojects.framework.core.event.EventPublishException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Kafka 事件发布实现
 */
public class KafkaEventPublisher<T> implements EventPublisher<T> {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaEventProperties properties;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaEventProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(@NonNull DomainEvent<T> event) {
        String topic = resolveTopic(event);
        try {
            log.debug("Publishing event to Kafka: topic={}, eventId={}", topic, event.eventId());
            kafkaTemplate.send(topic, event.eventId(), event.payload()).get();
            log.debug("Successfully published event: topic={}, eventId={}", topic, event.eventId());
        } catch (KafkaException e) {
            log.error("Failed to publish event to Kafka: topic={}, eventId={}, error={}",
                    topic, event.eventId(), e.getMessage(), e);
            throw new EventPublishException(
                    String.format("Failed to publish event [eventId=%s] to topic [%s]: %s",
                            event.eventId(), topic, e.getMessage()), e);
        } catch (ExecutionException e) {
            log.error("Unexpected error while publishing event to Kafka: topic={}, eventId={}, error={}",
                    topic, event.eventId(), e.getMessage(), e);
            throw new EventPublishException(
                    String.format("Unexpected error while publishing event [eventId=%s] to topic [%s]: %s",
                            event.eventId(), topic, e.getCause() != null ? e.getCause().getMessage() : e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while publishing event to Kafka: topic={}, eventId={}, error={}",
                    topic, event.eventId(), e.getMessage(), e);
            throw new EventPublishException(
                    String.format("Interrupted while publishing event [eventId=%s] to topic [%s]: %s",
                            event.eventId(), topic, e.getMessage()), e);
        }
    }

    @Override
    public CompletableFuture<Void> publishAsync(@NonNull DomainEvent<T> event) {
        String topic = resolveTopic(event);
        log.debug("Publishing event asynchronously to Kafka: topic={}, eventId={}", topic, event.eventId());
        return kafkaTemplate.send(topic, event.eventId(), event.payload())
                .thenApply(result -> {
                    log.debug("Successfully published event asynchronously: topic={}, eventId={}", topic, event.eventId());
                    return (Void) null;
                })
                .exceptionally(throwable -> {
                    log.error("Failed to publish event asynchronously to Kafka: topic={}, eventId={}, error={}",
                            topic, event.eventId(), throwable.getMessage(), throwable);
                    throw new EventPublishException(
                            String.format("Failed to publish event [eventId=%s] to topic [%s]: %s",
                                    event.eventId(), topic, throwable.getMessage()), throwable);
                });
    }

    private String resolveTopic(DomainEvent<T> event) {
        return event.topic() != null && !event.topic().isEmpty()
                ? event.topic()
                : properties.getDefaultTopic();
    }
}
