package io.github.afgprojects.framework.ai.core.resilience.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.Nullable;

/**
 * AI 韧性注解。
 * <p>
 * 标注在方法上，提供重试、熔断和降级能力。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiResilient {
    /** 重试次数 */
    int retry() default 3;
    /** 重试间隔（毫秒） */
    long retryIntervalMs() default 1000;
    /** 熔断器名称 */
    @Nullable String circuitBreaker() default "";
    /** 降级方法名 */
    @Nullable String fallbackMethod() default "";
}
