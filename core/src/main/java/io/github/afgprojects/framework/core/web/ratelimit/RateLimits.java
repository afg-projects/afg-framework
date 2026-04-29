package io.github.afgprojects.framework.core.web.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解容器
 * <p>
 * 支持在同一个方法上配置多个限流规则
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimits {

    /**
     * 限流注解数组
     *
     * @return 限流注解数组
     */
    RateLimit[] value();
}
