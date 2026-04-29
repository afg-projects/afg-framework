package io.github.afgprojects.framework.core.audit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * 审计日志配置属性
 * <p>
 * 配置项前缀: afg.audit
 * </p>
 *
 * <pre>
 * afg.audit.enabled=true
 * afg.audit.storage-type=redis
 * afg.audit.max-size=10000
 * afg.audit.ttl=7d
 * afg.audit.multi-tenant=true
 * afg.audit.sensitive-fields=password,token,secret
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.audit")
public class AuditLogProperties {

    /** 是否启用审计日志 */
    private boolean enabled = true;

    /** 存储类型（redis/log/none） */
    private StorageType storageType = StorageType.LOG;

    /** 最大保留条数（每个租户） */
    private int maxSize = 10000;

    /** 日志保留时间 */
    private Duration ttl = Duration.ofDays(7);

    /** 是否多租户模式 */
    private boolean multiTenant = true;

    /** 默认敏感字段列表（全小写，不含下划线） */
    private String[] sensitiveFields = {"password", "token", "secret", "apikey", "credential", "accesstoken"};

    /** 存储类型枚举 */
    public enum StorageType {
        /** Redis 存储 */
        REDIS,
        /** 数据库存储 */
        DATABASE,
        /** 仅记录日志 */
        LOG,
        /** 禁用存储（仅打印日志） */
        NONE
    }
}
