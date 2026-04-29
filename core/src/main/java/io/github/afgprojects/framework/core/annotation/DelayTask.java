package io.github.afgprojects.framework.core.annotation;

import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;

/**
 * 延迟任务注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DelayTask {
    String queue();
    @Nullable String description() default "";
    int concurrency() default 1;
    boolean enabled() default true;
}
