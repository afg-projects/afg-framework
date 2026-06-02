package io.github.afgprojects.framework.ai.core.performance.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiRateLimited {
    /** 限流 key */
    String value() default "";
    /** 每秒允许的请求数 */
    double permitsPerSecond() default 10.0;
    /** 超时等待时间（毫秒），0 表示不等待直接拒绝 */
    long timeoutMs() default 0;
}
