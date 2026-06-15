package io.github.afgprojects.framework.data.core.event;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * NoOp 审计追踪存储实现（默认降级）。
 * <p>
 * 仅记录日志，不执行实际持久化。当业务应用实现了自己的
 * {@link AuditTrailStorage} 时，此 NoOp 通过 {@code @ConditionalOnMissingBean} 自动退让。
 *
 * <p>生产环境应替换为真实的存储实现（如 JDBC 双表存储、文件存储或消息队列）。
 *
 * @see AuditTrailStorage
 */
@Slf4j
public class NoOpAuditTrailStorage implements AuditTrailStorage {

    @Override
    public void save(EntityChangedEvent<?> event, List<FieldChangeDiff> diffs) {
        log.debug("Audit trail not configured — skipping save for {} event on {} ({} field diffs)",
            event.getChangeType(),
            event.getEntityType().getSimpleName(),
            diffs.size());
    }
}
