package io.github.afgprojects.framework.data.core.tenant;

/**
 * 基于ThreadLocal的租户上下文实现
 */
public class ThreadLocalTenantContext implements TenantContext {

    private static final ThreadLocal<String> TENANT_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_TENANT_HOLDER = ThreadLocal.withInitial(() -> false);

    @Override
    public String getTenantId() {
        return TENANT_ID_HOLDER.get();
    }

    @Override
    public void setTenantId(String tenantId) {
        if (tenantId == null) {
            TENANT_ID_HOLDER.remove();
        } else {
            TENANT_ID_HOLDER.set(tenantId);
        }
    }

    @Override
    public void clear() {
        TENANT_ID_HOLDER.remove();
        IGNORE_TENANT_HOLDER.remove();
    }

    @Override
    public boolean isIgnoreTenant() {
        return IGNORE_TENANT_HOLDER.get();
    }

    @Override
    public void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT_HOLDER.set(ignore);
    }
}
