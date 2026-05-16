package io.github.afgprojects.framework.security.auth.tenant.model;

import io.github.afgprojects.framework.security.core.tenant.TenantContext;
import org.jspecify.annotations.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * 简单租户上下文实现。
 *
 * <p>提供基本的租户上下文信息，仅存储租户 ID。
 * 适用于简单的租户场景。
 *
 * @since 1.0.0
 */
public final class SimpleTenantContext implements TenantContext {

    private final String tenantId;

    /**
     * 构造简单租户上下文。
     *
     * @param tenantId 租户 ID，不能为 null
     */
    public SimpleTenantContext(@NonNull String tenantId) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId must not be null");
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
        SimpleTenantContext that = (SimpleTenantContext) o;
        return Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId);
    }

    @Override
    public String toString() {
        return "SimpleTenantContext{" +
            "tenantId='" + tenantId + '\'' +
            '}';
    }
}
