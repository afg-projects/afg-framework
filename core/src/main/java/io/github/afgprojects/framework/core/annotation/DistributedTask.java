package io.github.afgprojects.framework.core.annotation;

import java.lang.annotation.*;
import org.jspecify.annotations.Nullable;

/**
 * 分布式定时任务注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DistributedTask {
    String id();
    String cron();
    @Nullable String description() default "";
    long lockWaitTime() default 0;
    long lockLeaseTime() default -1;
    int shardCount() default 1;
    boolean enabled() default true;
}
