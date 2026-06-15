package io.github.afgprojects.framework.data.core.event;

import java.util.List;

/**
 * 审计追踪存储 SPI 接口。
 * <p>
 * 定义实体变更审计记录的持久化契约。框架通过此 SPI 将
 * {@link EntityChangedEvent} 连同计算出的字段级差异持久化存储。
 *
 * <h3>实现示例</h3>
 * <pre>{@code
 * @Component
 * public class DatabaseAuditTrailStorage implements AuditTrailStorage {
 *     @Override
 *     public void save(EntityChangedEvent<?> event, List<FieldChangeDiff> diffs) {
 *         // INSERT INTO data_change_log + data_change_field
 *     }
 * }
 * }</pre>
 *
 * @see EntityChangedEvent
 * @see FieldChangeDiff
 */
public interface AuditTrailStorage {

    /**
     * 保存实体变更审计记录。
     *
     * @param event 实体变更事件
     * @param diffs 字段级变更差异列表（对于 CREATED/DELETED，仅包含有值的字段）
     */
    void save(EntityChangedEvent<?> event, List<FieldChangeDiff> diffs);
}
