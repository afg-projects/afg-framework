package io.github.afgprojects.framework.core.audit;

/**
 * 审计日志存储接口
 * <p>
 * 定义审计日志的存储方式，支持多种存储后端实现
 * </p>
 *
 * <p>实现示例：</p>
 * <ul>
 *   <li>{@code io.github.afgprojects.impl.redis.audit.RedisAuditLogStorage} - 基于 Redis 的存储（推荐，在 afg-redis 模块中）</li>
 *   <li>{@code io.github.afgprojects.integration.jdbc.audit.DatabaseAuditLogStorage} - 基于数据库的存储（在 afg-jdbc 模块中）</li>
 *   <li>{@link LogAuditLogStorage} - 基于日志的存储</li>
 *   <li>{@link NoOpAuditLogStorage} - 空实现（禁用存储）</li>
 * </ul>
 */
@FunctionalInterface
public interface AuditLogStorage {

    /**
     * 保存审计日志
     *
     * @param auditLog 审计日志
     */
    void save(AuditLog auditLog);
}
