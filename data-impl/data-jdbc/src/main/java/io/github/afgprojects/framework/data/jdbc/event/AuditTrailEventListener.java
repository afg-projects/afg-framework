package io.github.afgprojects.framework.data.jdbc.event;

import io.github.afgprojects.framework.data.core.event.AuditTrailStorage;
import io.github.afgprojects.framework.data.core.event.EntityChangedEvent;
import io.github.afgprojects.framework.data.core.event.FieldChangeDiff;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadataCache;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 审计追踪事件监听器 — 事件驱动的审计数据持久化。
 * <p>
 * 监听 {@link EntityChangedEvent}，计算字段级差异（UPDATE 操作），
 * 并通过 {@link AuditTrailStorage} SPI 持久化审计记录。
 *
 * <h3>差异计算</h3>
 * <p>
 * 对于 UPDATE 操作，通过反射读取 oldEntity 和 newEntity 的字段值，
 * 逐字段对比，记录发生变化的字段及其前后值。
 * 对于 CREATED/DELETED/RESTORED，仅记录有值的字段（新值）。
 *
 * <h3>异步写入</h3>
 * <p>
 * 默认使用 Spring 的 {@link Async} 异步写入，避免阻塞业务事务。
 * 通过 {@code afg.data.audit.sync=true} 可切换为同步模式。
 *
 * @see AuditTrailStorage
 * @see EntityChangedEvent
 * @see FieldChangeDiff
 */
@Slf4j
public class AuditTrailEventListener {

    private final AuditTrailStorage storage;
    private final EntityMetadataCache metadataCache;
    private final boolean syncMode;

    /**
     * 创建审计追踪事件监听器。
     *
     * @param storage       审计存储实现
     * @param metadataCache 实体元数据缓存
     * @param syncMode      是否同步写入（true = 同步，false = 异步）
     */
    public AuditTrailEventListener(AuditTrailStorage storage,
                                   EntityMetadataCache metadataCache,
                                   boolean syncMode) {
        this.storage = storage;
        this.metadataCache = metadataCache;
        this.syncMode = syncMode;
    }

    /**
     * 监听实体变更事件并计算字段级差异。
     *
     * @param event 实体变更事件
     */
    @EventListener
    public void onEntityChanged(EntityChangedEvent<?> event) {
        if (syncMode) {
            handleEvent(event);
        } else {
            handleEventAsync(event);
        }
    }

    @Async
    public void handleEventAsync(EntityChangedEvent<?> event) {
        handleEvent(event);
    }

    private void handleEvent(EntityChangedEvent<?> event) {
        try {
            List<FieldChangeDiff> diffs = computeDiffs(event);
            storage.save(event, diffs);
        } catch (Exception e) {
            log.warn("Failed to persist audit trail for {} event on {}: {}",
                event.getChangeType(), event.getEntityType().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 计算字段级差异。
     * <p>
     * 对于 UPDATED 事件且 oldEntity 不为 null，逐字段对比新旧值，
     * 记录发生变化的前后值。对于 CREATED/DELETED/RESTORED，
     * 记录所有有值的字段。
     */
    private List<FieldChangeDiff> computeDiffs(EntityChangedEvent<?> event) {
        List<FieldChangeDiff> diffs = new ArrayList<>();
        EntityMetadata<?> metadata = getMetadata(event.getEntityType());

        if (metadata == null) {
            return diffs;
        }

        if (event.getChangeType() == EntityChangedEvent.ChangeType.UPDATED && event.getOldEntity() != null) {
            // UPDATE: 对比新旧值
            List<? extends FieldMetadata> fields = metadata.getFields();
            for (FieldMetadata fm : fields) {
                String fieldName = fm.getPropertyName();
                String oldValue = getFieldValue(event.getOldEntity(), fieldName);
                String newValue = getFieldValue(event.getEntity(), fieldName);
                if (!Objects.equals(oldValue, newValue)) {
                    diffs.add(FieldChangeDiff.of(fieldName, fm.getColumnName(), oldValue, newValue));
                }
            }
        } else if (event.getChangeType() == EntityChangedEvent.ChangeType.CREATED
                   || event.getChangeType() == EntityChangedEvent.ChangeType.DELETED
                   || event.getChangeType() == EntityChangedEvent.ChangeType.RESTORED) {
            // CREATED/DELETED/RESTORED: 记录有值的字段
            Object entity = event.getEntity();
            List<? extends FieldMetadata> fields = metadata.getFields();
            for (FieldMetadata fm : fields) {
                String fieldName = fm.getPropertyName();
                String value = getFieldValue(entity, fieldName);
                if (value != null) {
                    String oldLabel = event.getChangeType() == EntityChangedEvent.ChangeType.CREATED ? null : value;
                    String newLabel = event.getChangeType() == EntityChangedEvent.ChangeType.DELETED ? null : value;
                    diffs.add(FieldChangeDiff.of(fieldName, fm.getColumnName(), oldLabel, newLabel));
                }
            }
        }

        return diffs;
    }

    @Nullable
    private EntityMetadata<?> getMetadata(Class<?> entityType) {
        try {
            return metadataCache.get(entityType);
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    private String getFieldValue(Object entity, String fieldName) {
        try {
            Field field = findField(entity.getClass(), fieldName);
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object value = field.get(entity);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 遍历类层次结构查找字段。
     */
    @Nullable
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
