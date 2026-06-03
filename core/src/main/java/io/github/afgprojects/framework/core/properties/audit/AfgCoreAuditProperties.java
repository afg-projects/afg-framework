package io.github.afgprojects.framework.core.properties.audit;

import java.time.Duration;

import lombok.Data;

/**
 * 审计日志配置。
 */
@Data
public class AfgCoreAuditProperties {

    /**
     * 是否启用审计日志。
     */
    private boolean enabled = true;

    /**
     * 存储类型。
     */
    private AuditStorageType storageType = AuditStorageType.LOG;

    /**
     * 最大保留条数。
     */
    private int maxSize = 10000;

    /**
     * 日志保留时间。
     */
    private Duration ttl = Duration.ofDays(7);

    /**
     * 是否多租户模式。
     */
    private boolean multiTenant = true;

    /**
     * 默认敏感字段列表。
     */
    private String[] sensitiveFields = {"password", "token", "secret", "apikey", "credential", "accesstoken"};
}
