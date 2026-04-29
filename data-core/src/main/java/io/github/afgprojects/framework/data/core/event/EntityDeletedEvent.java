package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;

import java.time.Instant;

/**
 * 实体删除事件
 */
public final class EntityDeletedEvent<T> extends EntityEvent<T> {
    public EntityDeletedEvent(T entity, EntityMetadata<T> metadata, Instant timestamp) {
        super(entity, metadata, timestamp);
    }
}
