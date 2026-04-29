package io.github.afgprojects.framework.core.web.metrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法执行时间度量注解
 * <p>
 * 标注在方法上，自动记录方法执行时间分布
 * 使用 Timer 记录执行时间，支持百分位统计
 * </p>
 *
 * <pre>{@code
 * @TimedMetric(name = "api.user.query", description = "User query time")
 * public User getUser(String id) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimedMetric {

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

    /**
     * 百分位配置
     * <p>
     * 默认记录 P50, P95, P99
     * </p>
     *
     * @return 百分位数组
     */
    double[] percentiles() default {0.5, 0.95, 0.99};
}
