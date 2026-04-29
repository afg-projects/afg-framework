package io.github.afgprojects.framework.core.web.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法执行计数度量注解
 * <p>
 * 标注在方法上，自动记录方法调用次数
 * 使用 Counter 记录调用次数，包含成功和失败次数
 * </p>
 *
 * <pre>{@code
 * @CountedMetric(name = "api.user.query", description = "User query count")
 * public User getUser(String id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CountedMetric {

    /**
     * 指标名称
     * <p>
     * 如果为空，自动使用 "className.methodName" 格式
     * </p>
     *
     * @return 指标名称
     */
    String name() default "";

    /**
     * 指标描述
     *
     * @return 指标描述
     */
    String description() default "";
}
