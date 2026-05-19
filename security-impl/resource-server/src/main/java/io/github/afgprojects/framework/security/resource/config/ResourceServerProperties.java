package io.github.afgprojects.framework.security.resource.config;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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
    @Setter
    @Getter
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
    @Getter
    @Setter
    private boolean failIfTenantUnresolved = true;

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

    /**
     * 权限校验配置。
     */
    @Getter
    private final PermissionConfig permission = new PermissionConfig();

    /**
     * 权限校验配置。
     */
    @Getter
    @Setter
    public static class PermissionConfig {

        /**
         * 认证服务器地址。
         * 配置后将启用远程权限校验。
         */
        @Nullable
        private String authServerUrl;

        /**
         * 服务间调用密钥标识。
         * 用于签名验证，需与认证服务器配置的密钥标识一致。
         */
        @Nullable
        private String keyId;

        /**
         * 服务间调用密钥。
         * 用于生成签名，需与认证服务器配置的密钥一致。
         */
        @Nullable
        private String secret;
    }

}