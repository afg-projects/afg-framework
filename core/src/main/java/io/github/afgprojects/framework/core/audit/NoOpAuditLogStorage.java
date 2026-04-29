package io.github.afgprojects.framework.core.audit;

/**
 * 空审计日志存储
 * <p>
 * 禁用审计日志存储，仅打印调试日志
 * </p>
 */
public class NoOpAuditLogStorage implements AuditLogStorage {

    @Override
    public void save(AuditLog auditLog) {
        // 不执行任何存储操作
    }
}
