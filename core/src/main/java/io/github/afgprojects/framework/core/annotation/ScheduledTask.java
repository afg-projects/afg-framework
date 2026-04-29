package io.github.afgprojects.framework.core.annotation;

import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;

/**
 * 声明式定时任务注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ScheduledTask {
    String id();
    @Nullable String cron() default "";
    long fixedRate() default -1;
    long fixedDelay() default -1;
    long initialDelay() default 0;
    @Nullable String description() default "";
    boolean enabled() default true;
    long timeout() default -1;
    ErrorHandling errorHandling() default ErrorHandling.LOG_AND_CONTINUE;

    enum ErrorHandling {
        LOG_AND_CONTINUE,
        STOP_ON_ERROR,
        RETRY
    }
}
