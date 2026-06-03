package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * Observability 审计日志配置。
 */
@Data
public class ObservabilityAuditConfig {

    /**
     * Whether audit logging is enabled.
     */
    private boolean enabled = true;

    /**
     * Whether to include response bodies in audit logs.
     */
    private boolean includeResponses = false;

    /**
     * Maximum body length to log (in characters).
     */
    private int maxBodyLength = 10000;

    /**
     * Maximum number of audit log entries to retain (in-memory store).
     */
    private int maxEntries = 10000;
}
