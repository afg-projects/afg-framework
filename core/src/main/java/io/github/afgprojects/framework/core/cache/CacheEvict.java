package io.github.afgprojects.framework.core.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存清除注解
 * <p>
 * 标注在方法上，方法执行后清除缓存
 * </p>
 *
 * <pre>{@code
 * @CacheEvict(cacheName = "users", key = "#id")
 * public void deleteUser(String id) {
 *     userRepository.deleteById(id);
 * }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheEvict {

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
     * 是否清除整个缓存区域
     * <p>
     * 为 true 时忽略 key，清除 cacheName 下的所有缓存
     * </p>
     *
     * @return 是否清除全部
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前清除缓存
     * <p>
     * 默认 false，在方法执行成功后清除
     * </p>
     *
     * @return 是否在方法执行前清除
     */
    boolean beforeInvocation() default false;

    /**
     * 清除条件 SpEL 表达式
     *
     * @return 条件表达式
     */
    String condition() default "";
}