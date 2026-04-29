package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;

import java.time.Instant;

/**
 * 实体更新事件
 */
public final class EntityUpdatedEvent<T> extends EntityEvent<T> {
    public EntityUpdatedEvent(T entity, EntityMetadata<T> metadata, Instant timestamp) {
        super(entity, metadata, timestamp);
    }
}
