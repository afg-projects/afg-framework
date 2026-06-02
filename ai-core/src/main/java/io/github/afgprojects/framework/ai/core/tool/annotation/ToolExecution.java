package io.github.afgprojects.framework.ai.core.tool.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolExecution {
    /** 工具名称 */
    String value() default "";
    /** 是否记录执行日志 */
    boolean audited() default true;
    /** 超时时间（毫秒） */
    long timeoutMs() default 30000;
}
