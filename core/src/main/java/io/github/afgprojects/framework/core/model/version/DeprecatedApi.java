package io.github.afgprojects.framework.core.model.version;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.NonNull;

/**
 * 标记已废弃的 API
 * 提供废弃信息和迁移指导
 *
 * <p>使用示例:
 * <pre>{@code
 * @DeprecatedApi(
 *     since = "1.5.0",
 *     removedIn = "2.0.0",
 *     replacement = "use newMethod() instead",
 *     reason = "性能问题，新方法使用更高效的算法"
 * )
 * public void oldMethod() { }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DeprecatedApi {

    /**
     * 废弃起始版本
     */
    @NonNull String since();

    /**
     * 计划移除的版本
     */
    @NonNull String removedIn() default "";

    /**
     * 替代方案
     */
    @NonNull String replacement() default "";

    /**
     * 废弃原因
     */
    @NonNull String reason() default "";
}
