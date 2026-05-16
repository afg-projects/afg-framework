package io.github.afgprojects.framework.security.auth.tenant.resolver;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * 默认租户上下文实现。
 *
 * <p>用于 DefaultTenantResolver，标记为默认租户。
 *
 * @since 1.0.0
 */
public final class DefaultTenantContext implements TenantContext {

    private final String tenantId;
    private final boolean isDefault;

    /**
     * 构造默认租户上下文。
     *
     * @param tenantId 租户 ID，不能为 null
     */
    public DefaultTenantContext(@NonNull String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
        this.isDefault = true;
    }

    @Override
    @NonNull
    public String getTenantId() {
        return tenantId;
    }

    @Override
    @NonNull
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
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
            ", isDefault=" + isDefault +
            '}';
    }
}