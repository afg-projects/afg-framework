package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;

import java.time.Instant;

/**
 * 实体创建事件
 */
public final class EntityCreatedEvent<T> extends EntityEvent<T> {
    public EntityCreatedEvent(T entity, EntityMetadata<T> metadata, Instant timestamp) {
        super(entity, metadata, timestamp);
    }
}
