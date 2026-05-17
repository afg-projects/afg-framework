package io.github.afgprojects.framework.ai.core.resilience;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 降级策略接口
 *
 * <p>当主操作失败时提供备用方案：
 * <ul>
 *   <li>返回默认值</li>
 *   <li>调用备用服务</li>
 *   <li>返回缓存结果</li>
 * </ul>
 *
 * @param <T> 返回类型
 * @author afg-projects
 * @since 1.0.0
 */
public interface FallbackStrategy<T> {

    /**
     * 执行降级操作
     *
     * @param exception  导致失败的异常
     * @param context    降级上下文（包含原始请求信息）
     * @return 降级结果
     */
    @Nullable
    T fallback(@NonNull Exception exception, @NonNull FallbackContext context);

    /**
     * 判断是否应该执行降级
     *
     * @param exception 导致失败的异常
     * @return 是否应该降级
     */
    boolean shouldFallback(@NonNull Exception exception);

    /**
     * 降级上下文
     */
    interface FallbackContext {
        /**
         * 获取原始请求
         */
        @Nullable
        Object getOriginalRequest();

        /**
         * 获取请求 ID
         */
        @NonNull
        String getRequestId();

        /**
         * 获取请求时间戳
         */
        long getRequestTimestamp();
    }
}