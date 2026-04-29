package io.github.afgprojects.framework.core.security.datascope;

import org.jspecify.annotations.Nullable;
import org.springframework.core.NamedThreadLocal;

/**
 * 数据权限上下文持有者
 * <p>
 * 基于 ThreadLocal 存储当前请求的数据权限上下文。
 * 在请求开始时设置上下文，请求结束时清除。
 * <p>
 * 典型使用场景：
 * <pre>
 * // 在过滤器或拦截器中设置
 * DataScopeContextHolder.setContext(dataScopeContext);
 *
 * // 在业务代码中获取
 * DataScopeContext context = DataScopeContextHolder.getContext();
 *
 * // 请求结束时清除
 * DataScopeContextHolder.clear();
 * </pre>
 */
public final class DataScopeContextHolder {

    private static final ThreadLocal<DataScopeContext> CONTEXT_HOLDER =
            new NamedThreadLocal<>("DataScope Context");

    private DataScopeContextHolder() {
        // 私有构造函数，防止实例化
    }

    /**
     * 设置当前数据权限上下文
     *
     * @param context 数据权限上下文
     */
    public static void setContext(@Nullable DataScopeContext context) {
        if (context == null) {
            clear();
        } else {
            CONTEXT_HOLDER.set(context);
        }
    }

    /**
     * 获取当前数据权限上下文
     *
     * @return 数据权限上下文，如果未设置则返回 null
     */
    public static @Nullable DataScopeContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前数据权限上下文，如果未设置则返回空上下文
     *
     * @return 数据权限上下文，不会返回 null
     */
    public static DataScopeContext getRequiredContext() {
        DataScopeContext context = getContext();
        return context != null ? context : DataScopeContext.empty();
    }

    /**
     * 清除当前数据权限上下文
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 在指定上下文中执行操作
     * <p>
     * 执行完成后自动恢复原上下文
     *
     * @param context  数据权限上下文
     * @param runnable 要执行的操作
     */
    public static void runWithContext(DataScopeContext context, Runnable runnable) {
        DataScopeContext oldContext = getContext();
        try {
            setContext(context);
            runnable.run();
        } finally {
            setContext(oldContext);
        }
    }

    /**
     * 在忽略数据权限的上下文中执行操作
     * <p>
     * 用于系统操作或跨权限查询场景
     *
     * @param runnable 要执行的操作
     */
    public static void runWithoutDataScope(Runnable runnable) {
        DataScopeContext context = getContext();
        if (context == null) {
            context = DataScopeContext.empty();
        }
        DataScopeContext ignoreContext = DataScopeContext.builder()
                .userId(context.getUserId())
                .deptId(context.getDeptId())
                .accessibleDeptIds(context.getAccessibleDeptIds())
                .ignoreDataScope(true)
                .build();
        runWithContext(ignoreContext, runnable);
    }
}