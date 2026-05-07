package io.github.afgprojects.framework.security.resource.config;

import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 资源服务器通用配置属性。
 *
 * <p>配置资源服务器的通用参数。
 *
 * <p>配置示例：
 * <pre>
 * afg.security.resource.enabled=true
 * afg.security.resource.tenant-strategies=token,header
 * afg.security.resource.tenant-header-name=X-Tenant-Id
 * </pre>
 *
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "afg.security.resource")
public class ResourceServerProperties {

    /**
     * 是否启用资源服务器。
     * 默认启用。
     */
    private boolean enabled = true;

    /**
     * 租户解析策略列表。
     * 默认按 token, header 顺序解析。
     */
    private List<String> tenantStrategies = List.of("token", "header");

    /**
     * 租户请求头名称。
     * 默认 X-Tenant-Id。
     */
    private String tenantHeaderName = "X-Tenant-Id";

    /**
     * 无法解析租户时是否抛出异常。
     * 默认 true。
     */
    private boolean failIfTenantUnresolved = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    public List<String> getTenantStrategies() {
        return tenantStrategies;
    }

    public void setTenantStrategies(@NonNull List<String> tenantStrategies) {
        this.tenantStrategies = tenantStrategies;
    }

    @NonNull
    public String getTenantHeaderName() {
        return tenantHeaderName;
    }

    public void setTenantHeaderName(@NonNull String tenantHeaderName) {
        this.tenantHeaderName = tenantHeaderName;
    }

    public boolean isFailIfTenantUnresolved() {
        return failIfTenantUnresolved;
    }

    public void setFailIfTenantUnresolved(boolean failIfTenantUnresolved) {
        this.failIfTenantUnresolved = failIfTenantUnresolved;
    }
}