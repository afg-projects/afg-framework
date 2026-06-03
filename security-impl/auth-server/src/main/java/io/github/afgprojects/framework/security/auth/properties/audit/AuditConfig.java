package io.github.afgprojects.framework.security.auth.properties.audit;

import java.util.List;

import lombok.Data;

/**
 * 审计配置。
 */
@Data
public class AuditConfig {

    /**
     * 是否启用审计功能。
     */
    private boolean enabled = true;

    /**
     * 告警配置。
     */
    private AlertConfig alert = new AlertConfig();
}
