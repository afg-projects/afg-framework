package io.github.afgprojects.framework.integration.redis.audit;

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.audit.AuditLog;
import io.github.afgprojects.framework.core.audit.AuditLogProperties;
import io.github.afgprojects.framework.core.audit.AuditLogStorage;
import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 基于 Redis 的审计日志存储
 * <p>
 * 使用 Redisson 客户端将审计日志存储到 Redis List 中
 * 支持配置最大保留条数和过期时间
 * </p>
 *
 * <p>存储结构：</p>
 * <ul>
 *   <li>Key 格式: audit:log:{tenantId} 或 audit:log:global</li>
 *   <li>Value: JSON 格式的审计日志</li>
 *   <li>使用 RList 实现，支持限制最大条数</li>
 * </ul>
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class RedisAuditLogStorage implements AuditLogStorage {

    private static final Logger log = LoggerFactory.getLogger(RedisAuditLogStorage.class);

    private static final String KEY_PREFIX = "audit:log:";
    private static final String GLOBAL_KEY = "global";

    private final RedissonClient redissonClient;
    private final AuditLogProperties properties;

    /**
     * 构造函数
     *
     * @param redissonClient Redisson 客户端
     * @param properties     审计日志配置
     */
    public RedisAuditLogStorage(@NonNull RedissonClient redissonClient, @NonNull AuditLogProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public void save(@NonNull AuditLog auditLog) {
        try {
            String key = buildKey(auditLog.tenantId());
            String json = JacksonUtils.toJson(auditLog);

            RList<String> list = redissonClient.getList(key);

            // 添加到列表头部（最新的在前面）
            list.addFirst(json);

            // 限制最大条数
            int maxSize = properties.getMaxSize();
            while (list.size() > maxSize) {
                list.removeLast();
            }

            // 设置过期时间
            Duration ttl = properties.getTtl();
            if (ttl != null && !ttl.isZero()) {
                list.expire(ttl);
            }

            log.debug("Audit log saved: id={}, operation={}", auditLog.id(), auditLog.operation());
        } catch (Exception e) {
            // 存储失败不应影响业务流程，仅记录错误日志
            log.error("Failed to save audit log: id={}, operation={}", auditLog.id(), auditLog.operation(), e);
        }
    }

    /**
     * 构建存储 Key
     *
     * @param tenantId 租户 ID
     * @return Redis Key
     */
    private String buildKey(Long tenantId) {
        if (properties.isMultiTenant() && tenantId != null) {
            return KEY_PREFIX + tenantId;
        }
        return KEY_PREFIX + GLOBAL_KEY;
    }
}
