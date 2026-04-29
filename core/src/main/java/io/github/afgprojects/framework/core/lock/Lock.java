package io.github.afgprojects.framework.core.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 * <p>
 * 标注在方法上，自动在方法执行前获取锁，执行后释放锁。
 * 支持 SpEL 表达式动态生成锁的键。
 * </p>
 *
 * <pre>{@code
 * // 基本用法
 * @Lock(key = "user-update")
 * public void updateUser(User user) { ... }
 *
 * // 使用 SpEL 表达式
 * @Lock(key = "#userId")
 * public User getUser(String userId) { ... }
 *
 * // 指定锁类型和超时时间
 * @Lock(key = "order-#orderId", waitTime = 5000, leaseTime = 30000, lockType = LockType.FAIR)
 * public void processOrder(String orderId) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Lock {

    /**
     * 锁的键
     * <p>
     * 支持 SpEL 表达式，可以使用方法参数。
     * 例如：#userId、#user.id、#p0（第一个参数）
     * </p>
     *
     * @return 锁的键
     */
    String key();

    /**
     * 锁键前缀
     * <p>
     * 最终的锁键为：prefix + key
     * </p>
     *
     * @return 锁键前缀
     */
    String prefix() default "";

    /**
     * 等待时间
     * <p>
     * 获取锁的最大等待时间，超过此时间未获取到锁则抛出异常。
     * 默认值为 -1，表示使用全局配置。
     * </p>
     *
     * @return 等待时间
     */
    long waitTime() default -1;

    /**
     * 持有时间（租约时间）
     * <p>
     * 锁的自动释放时间。
     * 默认值为 -1，表示使用 watchdog 机制自动续期。
     * </p>
     *
     * @return 持有时间
     */
    long leaseTime() default -1;

    /**
     * 时间单位
     * <p>
     * waitTime 和 leaseTime 的时间单位
     * </p>
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 锁类型
     * <p>
     * 支持可重入锁、公平锁、读写锁
     * </p>
     *
     * @return 锁类型
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 获取锁失败时的错误消息
     *
     * @return 错误消息
     */
    String message() default "获取锁失败，请稍后重试";

    /**
     * 是否在获取锁失败时抛出异常
     * <p>
     * 如果为 false，方法将正常执行（不获取锁）。
     * 默认为 true，获取锁失败时抛出异常。
     * </p>
     *
     * @return 是否抛出异常
     */
    boolean throwOnFailure() default true;

    /**
     * 时间单位枚举
     */
    enum TimeUnit {
        /**
         * 毫秒
         */
        MILLISECONDS,
        /**
         * 秒
         */
        SECONDS,
        /**
         * 分钟
         */
        MINUTES
    }
}
