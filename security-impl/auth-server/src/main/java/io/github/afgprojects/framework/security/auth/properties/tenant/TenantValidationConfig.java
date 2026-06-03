package io.github.afgprojects.framework.security.auth.properties.tenant;

import java.time.Duration;

import lombok.Data;

/**
 * 租户验证配置。
 */
@Data
public class TenantValidationConfig {

    /**
     * 是否启用租户验证。
     */
    private boolean enabled = true;

    /**
     * 租户验证缓存 TTL。
     */
    private Duration cacheTtl = Duration.ofMinutes(5);
}
