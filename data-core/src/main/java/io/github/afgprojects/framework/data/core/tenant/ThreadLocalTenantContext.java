package io.github.afgprojects.framework.data.core.tenant;

/**
 * 基于ThreadLocal的租户上下文实现
 * <p>
 * 提供简单的租户上下文管理，适用于单线程场景。
 * <p>
 * <b>使用场景：</b>简单的 Web 请求处理、同步方法调用等不需要跨线程传播的场景。
 * <p>
 * <b>注意：</b>如果需要在异步执行、线程池等场景中使用租户上下文，
 * 请使用 {@link io.github.afgprojects.framework.data.core.context.TenantContextHolder}，
 * 它提供了快照/恢复机制和跨线程传播支持。
 *
 * @see io.github.afgprojects.framework.data.core.context.TenantContextHolder
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
