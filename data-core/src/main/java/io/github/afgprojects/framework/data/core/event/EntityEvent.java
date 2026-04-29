package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

/**
 * 实体事件基类
 * <p>
 * 继承 Spring ApplicationEvent，支持 Spring 事件机制
 */
public sealed abstract class EntityEvent<T> extends ApplicationEvent
    permits EntityCreatedEvent, EntityUpdatedEvent, EntityDeletedEvent,
            EntityBatchCreatedEvent, EntityBatchUpdatedEvent, EntityBatchDeletedEvent {

    private final T entity;
    private final EntityMetadata<T> metadata;
    private final Instant occurredAt;

    protected EntityEvent(T entity, EntityMetadata<T> metadata, Instant occurredAt) {
        super(entity);
        this.entity = entity;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    public T getEntity() {
        return entity;
    }

    public EntityMetadata<T> getMetadata() {
        return metadata;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
