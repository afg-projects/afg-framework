package io.github.afgprojects.framework.core.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 声明式缓存注解
 * <p>
 * 标注在方法上，自动缓存方法返回值
 * </p>
 *
 * <pre>{@code
 * @Cached(cacheName = "users", key = "#id", ttl = 60, timeUnit = TimeUnit.MINUTES)
 * public User getUser(String id) {
 *     return userRepository.findById(id);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Cached {

    /**
     * 缓存名称
     * <p>
     * 用于区分不同的缓存区域
     * </p>
     *
     * @return 缓存名称
     */
    String cacheName();

    /**
     * 缓存键的 SpEL 表达式
     * <p>
     * 支持以下格式：
     * <ul>
     *   <li>#参数名 - 引用方法参数</li>
     *   <li>#p0 或 #a0 - 引用第 1 个参数</li>
     *   <li>#result - 引用返回值（仅用于 @CacheEvict 的 beforeInvocation=false）</li>
     *   <li>字符串常量</li>
     * </ul>
     * 示例：
     * <ul>
     *   <li>#id</li>
     *   <li>#user.id</li>
     *   <li>#p0 + ':' + #p1</li>
     * </ul>
     * </p>
     *
     * @return SpEL 表达式，默认为空（使用所有参数生成 key）
     */
    String key() default "";

    /**
     * 缓存键前缀
     * <p>
     * 添加到 key 前面，便于区分不同场景
     * </p>
     *
     * @return 键前缀
     */
    String keyPrefix() default "";

    /**
     * 过期时间
     * <p>
     * 配合 timeUnit 使用，默认为 0 表示永不过期
     * </p>
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
     * <p>
     * 开启后可以防止缓存穿透
     * </p>
     *
     * @return 是否缓存 null 值
     */
    boolean cacheNull() default true;

    /**
     * 缓存条件 SpEL 表达式
     * <p>
     * 表达式计算结果为 true 时才缓存
     * 示例：#result != null、#id.length() > 5
     * </p>
     *
     * @return 条件表达式
     */
    String condition() default "";

    /**
     * 排除缓存条件 SpEL 表达式
     * <p>
     * 表达式计算结果为 true 时不缓存
     * </p>
     *
     * @return 排除条件表达式
     */
    String unless() default "";
}