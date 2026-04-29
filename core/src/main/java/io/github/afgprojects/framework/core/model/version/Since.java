package io.github.afgprojects.framework.core.model.version;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jspecify.annotations.NonNull;

/**
 * 标记 API 的引入版本
 * 用于 API 版本管理和兼容性检查
 *
 * <p>使用示例:
 * <pre>{@code
 * @Since("1.0.0")
 * public void newMethod() { }
 *
 * @Since(value = "1.2.0", note = "支持批量操作")
 * public void batchProcess(List<Item> items) { }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE, ElementType.FIELD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Since {

    /**
     * API 引入的版本号
     * 格式: major.minor.patch (如 1.0.0)
     */
    @NonNull String value();

    /**
     * 版本说明
     */
    @NonNull String note() default "";
}
