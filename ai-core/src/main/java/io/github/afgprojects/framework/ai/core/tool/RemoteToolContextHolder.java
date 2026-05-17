package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 远程工具上下文持有者。
 *
 * <p>使用 ThreadLocal 存储远程工具调用的安全上下文。
 * 远程服务端通过 {@link #setContext(ToolContext)} 设置上下文，
 * 业务代码通过 {@link #getContext()} 获取上下文。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 在 Filter 中设置上下文
 * RemoteToolContextHolder.setContext(context);
 *
 * try {
 *     // 业务代码获取上下文
 *     ToolContext context = RemoteToolContextHolder.getContext();
 *     String userId = context.getUserId();
 * } finally {
 *     // 清理上下文
 *     RemoteToolContextHolder.clear();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class RemoteToolContextHolder {

    private static final ThreadLocal<ToolContext> CONTEXT_HOLDER = new ThreadLocal<>();

    private RemoteToolContextHolder() {}

    /**
     * 设置当前上下文。
     *
     * @param context 工具上下文
     */
    public static void setContext(@NonNull ToolContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前上下文。
     *
     * @return 工具上下文，如果未设置返回 null
     */
    public static @Nullable ToolContext getContext() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 获取当前上下文（要求必须存在）。
     *
     * @return 工具上下文
     * @throws IllegalStateException 如果上下文未设置
     */
    public static @NonNull ToolContext getRequiredContext() {
        ToolContext context = getContext();
        if (context == null) {
            throw new IllegalStateException("RemoteToolContext not set");
        }
        return context;
    }

    /**
     * 清除当前上下文。
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    /**
     * 判断是否已设置上下文。
     *
     * @return 如果已设置返回 true
     */
    public static boolean hasContext() {
        return CONTEXT_HOLDER.get() != null;
    }
}
