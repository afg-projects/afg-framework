package io.github.afgprojects.framework.ai.core.properties.observability;

import lombok.Data;

/**
 * 审计日志配置。
 */
@Data
public class AuditConfig {

    /**
     * 是否启用审计日志。
     */
    private boolean enabled = true;

    /**
     * 最大审计条目数。
     */
    private int maxEntries = 10000;
}
