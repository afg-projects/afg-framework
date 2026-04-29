package io.github.afgprojects.framework.core.web.shutdown;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法为关闭回调，支持排序。
 * 标注此注解的方法将在优雅关闭时被调用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ShutdownOrder {
    /**
     * 关闭回调的阶段名称。
     * 阶段按照 ShutdownProperties 中定义的顺序执行。
     * 默认为 "cleanup"。
     */
    String phase() default "cleanup";

    /**
     * 阶段内的执行顺序。
     * 数值越小越先执行。
     * 默认为 0。
     */
    int order() default 0;
}
