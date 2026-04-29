package io.github.afgprojects.framework.data.core.context;

import io.github.afgprojects.framework.data.core.scope.TenantScope;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 租户上下文持有者
 * <p>
 * 基于 ThreadLocal 实现租户上下文管理，支持跨线程传播
 */
public class TenantContextHolder {

    private final ThreadLocal<String> tenantIdHolder = new ThreadLocal<>();

    /**
     * 获取当前租户ID
     */
    public @Nullable String getTenantId() {
        return tenantIdHolder.get();
    }

    /**
     * 设置当前租户ID
     */
    public void setTenantId(@Nullable String tenantId) {
        if (tenantId == null) {
            tenantIdHolder.remove();
        } else {
            tenantIdHolder.set(tenantId);
        }
    }

    /**
     * 清除租户上下文
     */
    public void clear() {
        tenantIdHolder.remove();
    }

    /**
     * 创建租户作用域
     */
    public TenantScope scope(String tenantId) {
        String previousTenantId = getTenantId();
        setTenantId(tenantId);
        return new DefaultTenantScope(tenantId, previousTenantId, this);
    }

    /**
     * 快照当前租户上下文
     * <p>
     * 用于跨线程传播租户上下文
     *
     * @return 当前租户上下文的快照，如果未设置则返回 null
     */
    public @Nullable TenantContextSnapshot snapshot() {
        String tenantId = getTenantId();
        if (tenantId == null) {
            return null;
        }
        return new TenantContextSnapshot(tenantId);
    }

    /**
     * 从快照恢复租户上下文
     * <p>
     * 将快照中的租户信息恢复到当前线程
     *
     * @param snapshot 租户上下文快照，可以为 null
     */
    public void restore(@Nullable TenantContextSnapshot snapshot) {
        if (snapshot == null) {
            clear();
        } else {
            setTenantId(snapshot.tenantId());
        }
    }

    /**
     * 在指定租户上下文中执行操作
     * <p>
     * 执行完成后自动恢复之前的租户上下文
     *
     * @param snapshot 租户上下文快照
     * @param runnable 要执行的操作
     */
    public void runWithSnapshot(@Nullable TenantContextSnapshot snapshot, @NonNull Runnable runnable) {
        String previousTenantId = getTenantId();
        try {
            restore(snapshot);
            runnable.run();
        } finally {
            if (previousTenantId == null) {
                clear();
            } else {
                setTenantId(previousTenantId);
            }
        }
    }

    /**
     * 租户上下文快照
     * <p>
     * 不可变对象，用于跨线程传播租户信息
     *
     * @param tenantId 租户ID
     */
    public record TenantContextSnapshot(@Nullable String tenantId) {
        /**
         * 检查快照是否有效
         *
         * @return 如果包含有效的租户ID则返回 true
         */
        public boolean isValid() {
            return tenantId != null;
        }
    }

    private record DefaultTenantScope(
        String tenantId,
        @Nullable String previousTenantId,
        TenantContextHolder holder
    ) implements TenantScope {

        @Override
        public String getTenantId() {
            return tenantId;
        }

        @Override
        public void close() {
            if (previousTenantId == null) {
                holder.clear();
            } else {
                holder.setTenantId(previousTenantId);
            }
        }
    }
}
