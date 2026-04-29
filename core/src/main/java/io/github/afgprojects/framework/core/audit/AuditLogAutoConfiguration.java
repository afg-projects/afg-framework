package io.github.afgprojects.framework.core.audit;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 审计日志自动配置
 * <p>
 * 自动配置条件:
 * <ul>
 *   <li>afg.audit.enabled=true（默认为 true）</li>
 *   <li>存在 AuditLogStorage Bean 或根据 storage-type 自动配置</li>
 * </ul>
 * </p>
 *
 * <p>存储类型配置：</p>
 * <ul>
 *   <li>redis: 需要 RedissonClient Bean，使用 Redis 存储（需引入 afg-redis 模块）</li>
 *   <li>database: 需要 DataSource Bean，使用数据库存储（需引入 afg-jdbc 模块）</li>
 *   <li>log: 输出到日志文件（默认）</li>
 *   <li>none: 禁用存储</li>
 * </ul>
 *
 * <p>注意：RedisAuditLogStorage 已移至 afg-redis 模块，DatabaseAuditLogStorage 已移至 afg-jdbc 模块</p>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "afg.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditLogProperties.class)
public class AuditLogAutoConfiguration {

    /**
     * 配置审计日志切面
     *
     * @param storage    审计日志存储
     * @param properties 审计日志配置属性
     * @return 审计日志切面实例
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditLogAspect auditLogAspect(@NonNull AuditLogStorage storage, @NonNull AuditLogProperties properties) {
        return new AuditLogAspect(storage, properties);
    }

    /**
     * 配置日志审计存储
     * <p>
     * 条件：storage-type=log（默认）或 Redis 存储不可用时
     * </p>
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "afg.audit",
            name = "storage-type",
            havingValue = "log",
            matchIfMissing = true)
    @ConditionalOnMissingBean
    public LogAuditLogStorage logAuditLogStorage() {
        return new LogAuditLogStorage();
    }

    /**
     * 配置空审计存储
     * <p>
     * 条件：storage-type=none
     * </p>
     */
    @Bean
    @ConditionalOnProperty(prefix = "afg.audit", name = "storage-type", havingValue = "none")
    @ConditionalOnMissingBean
    public NoOpAuditLogStorage noOpAuditLogStorage() {
        return new NoOpAuditLogStorage();
    }
}
