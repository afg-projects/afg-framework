package io.github.afgprojects.framework.security.core.tenant;

import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 默认租户上下文实现。
 *
 * <p>提供租户上下文的基本实现，包含租户 ID、租户编码和扩展属性。
 *
 * @since 1.0.0
 */
public class DefaultTenantContext implements TenantContext {

    private final @NonNull String tenantId;
    private final String tenantCode;
    private final String tenantName;
    private final Map<String, Object> attributes;
    private final boolean defaultTenant;
    private final boolean valid;

    /**
     * 构造租户上下文。
     *
     * @param tenantId 租户 ID，永不为 null
     */
    public DefaultTenantContext(@NonNull String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.tenantCode = null;
        this.tenantName = null;
        this.attributes = new HashMap<>();
        this.defaultTenant = false;
        this.valid = true;
    }

    /**
     * 构造租户上下文。
     *
     * @param tenantId 租户 ID，永不为 null
     * @param tenantCode 租户编码
     * @param tenantName 租户名称
     */
    public DefaultTenantContext(
            @NonNull String tenantId,
            String tenantCode,
            String tenantName) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.attributes = new HashMap<>();
        this.defaultTenant = false;
        this.valid = true;
    }

    /**
     * 构造租户上下文（完整参数）。
     *
     * @param tenantId 租户 ID，永不为 null
     * @param tenantCode 租户编码
     * @param tenantName 租户名称
     * @param attributes 扩展属性
     * @param defaultTenant 是否为默认租户
     * @param valid 租户是否有效
     */
    public DefaultTenantContext(
            @NonNull String tenantId,
            String tenantCode,
            String tenantName,
            Map<String, Object> attributes,
            boolean defaultTenant,
            boolean valid) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.tenantCode = tenantCode;
        this.tenantName = tenantName;
        this.attributes = attributes != null ? new HashMap<>(attributes) : new HashMap<>();
        this.defaultTenant = defaultTenant;
        this.valid = valid;
    }

    @Override
    @NonNull
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public String getTenantCode() {
        return tenantCode != null ? tenantCode : tenantId;
    }

    @Override
    public String getTenantName() {
        return tenantName;
    }

    @Override
    @NonNull
    public Map<String, Object> getAttributes() {
        return new HashMap<>(attributes);
    }

    @Override
    public boolean isDefault() {
        return defaultTenant;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    /**
     * 添加扩展属性。
     *
     * @param key 属性键
     * @param value 属性值
     * @return 当前租户上下文
     */
    public DefaultTenantContext addAttribute(String key, Object value) {
        this.attributes.put(key, value);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultTenantContext that = (DefaultTenantContext) o;
        return Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public String toString() {
        return "DefaultTenantContext{" +
                "tenantId='" + tenantId + '\'' +
                ", tenantCode='" + tenantCode + '\'' +
                ", tenantName='" + tenantName + '\'' +
                ", defaultTenant=" + defaultTenant +
                ", valid=" + valid +
                '}';
    }
}
