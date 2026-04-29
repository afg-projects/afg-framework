package io.github.afgprojects.framework.core.web.version;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.NonNull;

/**
 * 标记 API 版本
 * 用于运行时版本路由和兼容性检查
 *
 * <p>使用示例:
 * <pre>{@code
 * @RestController
 * @ApiVersion("1.0")
 * public class UserApiV1 {
 *     @GetMapping("/users")
 *     public List<User> getUsers() { ... }
 * }
 *
 * @RestController
 * @ApiVersion(value = "2.0", deprecated = true)
 * @RequestMapping("/v2/users")
 * public class UserApiV2 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 *
 * // 方法级版本标记
 * @ApiVersion(value = "1.5", since = "1.0", until = "2.0")
 * @GetMapping("/users/search")
 * public SearchResult searchUsers() { ... }
 * }</pre>
 *
 * <p>版本路由规则:
 * <ul>
 *   <li>类级注解应用于该类所有方法</li>
 *   <li>方法级注解覆盖类级注解</li>
 *   <li>支持多版本共存（如 v1 和 v2 同时存在）</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {

    /**
     * API 版本号
     * 格式: major.minor (如 "1.0", "2.1")
     * 主版本号用于兼容性检查，次版本号用于功能增强标识
     */
    @NonNull String value();

    /**
     * 是否已废弃
     * 废弃的 API 会在响应头中添加警告信息
     */
    boolean deprecated() default false;

    /**
     * API 引入版本
     * 用于版本兼容性检查
     */
    @NonNull String since() default "";

    /**
     * API 废弃版本（计划移除版本）
     * 当请求版本 >= 此版本时返回错误
     */
    @NonNull String until() default "";

    /**
     * 替代方案说明
     * 用于废弃 API 的迁移指导
     */
    @NonNull String replacement() default "";

    /**
     * 废弃原因
     */
    @NonNull String reason() default "";
}