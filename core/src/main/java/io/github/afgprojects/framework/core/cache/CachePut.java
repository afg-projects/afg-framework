package io.github.afgprojects.framework.core.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 缓存更新注解
 * <p>
 * 标注在方法上，方法执行后更新缓存
 * </p>
 *
 * <pre>{@code
 * @CachePut(cacheName = "users", key = "#user.id")
 * public User updateUser(User user) {
 *     return userRepository.save(user);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CachePut {

    /**
     * 缓存名称
     *
     * @return 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的 SpEL 表达式
     *
     * @return SpEL 表达式
     * @see Cached#key()
     */
    String key() default "";

    /**
     * 缓存键前缀
     *
     * @return 键前缀
     */
    String keyPrefix() default "";

    /**
     * 过期时间
     *
     * @return 过期时间
     */
    long ttl() default 0;

    /**
     * 过期时间单位
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    /**
     * 是否缓存 null 值
     *
     * @return 是否缓存 null 值
     */
    boolean cacheNull() default true;

    /**
     * 更新条件 SpEL 表达式
     *
     * @return 条件表达式
     */
    String condition() default "";

    /**
     * 排除更新条件 SpEL 表达式
     *
     * @return 排除条件表达式
     */
    String unless() default "";
}