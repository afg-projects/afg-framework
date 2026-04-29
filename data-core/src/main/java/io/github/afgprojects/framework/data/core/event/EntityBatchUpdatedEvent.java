package io.github.afgprojects.framework.data.core.event;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;

import java.time.Instant;
import java.util.List;

/**
 * 批量更新事件
 */
public final class EntityBatchUpdatedEvent<T> extends EntityEvent<T> {
    private final List<T> entities;

    public EntityBatchUpdatedEvent(T entity, EntityMetadata<T> metadata, Instant timestamp, List<T> entities) {
        super(entity, metadata, timestamp);
        this.entities = entities;
    }

    public List<T> getEntities() {
        return entities;
    }
}
