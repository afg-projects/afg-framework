package io.github.afgprojects.framework.ai.core.api.performance;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * 速率限制器接口
 *
 * <p>用于控制 AI 请求速率：
 * <ul>
 *   <li>令牌桶算法</li>
 *   <li>滑动窗口算法</li>
 *   <li>多维度限制（用户、租户、全局）</li>
 *   <li>等待队列</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface RateLimiter {

    /**
     * 尝试获取许可
     *
     * @param key 限制键（如用户 ID、租户 ID）
     * @return 是否获取成功
     */
    boolean tryAcquire(@NonNull String key);

    /**
     * 尝试获取多个许可
     *
     * @param key    限制键
     * @param permits 许可数量
     * @return 是否获取成功
     */
    boolean tryAcquire(@NonNull String key, int permits);

    /**
     * 获取许可（阻塞等待）
     *
     * @param key 限制键
     * @throws InterruptedException 中断异常
     */
    void acquire(@NonNull String key) throws InterruptedException;

    /**
     * 获取许可（带超时）
     *
     * @param key     限制键
     * @param timeout 超时时间
     * @return 是否获取成功
     * @throws InterruptedException 中断异常
     */
    boolean tryAcquire(@NonNull String key, @NonNull Duration timeout) throws InterruptedException;

    /**
     * 获取许可状态
     *
     * @param key 限制键
     * @return 许可状态
     */
    @NonNull
    RateLimitStatus getStatus(@NonNull String key);

    /**
     * 重置限制
     *
     * @param key 限制键
     */
    void reset(@NonNull String key);

    /**
     * 获取所有限制键
     *
     * @return 限制键列表
     */
    @NonNull
    List<String> getKeys();

    /**
     * 设置限制配置
     *
     * @param key    限制键
     * @param config 限制配置
     */
    void setConfig(@NonNull String key, @NonNull RateLimitConfig config);

    /**
     * 获取限制配置
     *
     * @param key 限制键
     * @return 限制配置
     */
    @Nullable
    RateLimitConfig getConfig(@NonNull String key);

    /**
     * 许可状态接口
     */
    interface RateLimitStatus {

        /**
         * 是否被限制
         *
         * @return 是否被限制
         */
        boolean isLimited();

        /**
         * 获取当前可用许可数
         *
         * @return 当前可用许可数
         */
        long getAvailablePermits();

        /**
         * 获取等待时间（直到下一个许可可用）
         *
         * @return 等待时间
         */
        @NonNull
        Duration getWaitTime();

        /**
         * 获取已使用许可数
         *
         * @return 已使用许可数
         */
        long getUsedPermits();

        /**
         * 获取重置时间
         *
         * @return 重置时间（毫秒时间戳）
         */
        long getResetTime();
    }

    /**
     * 限制配置接口
     */
    interface RateLimitConfig {

        /**
         * 获取许可数量
         *
         * @return 许可数量
         */
        long getPermits();

        /**
         * 获取时间窗口
         *
         * @return 时间窗口
         */
        @NonNull
        Duration getWindow();

        /**
         * 获取算法类型
         *
         * @return 算法类型
         */
        @NonNull
        Algorithm getAlgorithm();

        /**
         * 获取最大等待时间
         *
         * @return 最大等待时间，如果无限制返回 null
         */
        @Nullable
        Duration getMaxWaitTime();

        /**
         * 算法类型
         */
        enum Algorithm {
            /**
             * 令牌桶
             */
            TOKEN_BUCKET,
            /**
             * 滑动窗口
             */
            SLIDING_WINDOW,
            /**
             * 固定窗口
             */
            FIXED_WINDOW,
            /**
             * 漏桶
             */
            LEAKY_BUCKET
        }
    }
}