package io.github.afgprojects.framework.ai.core.observability.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiAudited {
    /** 操作类型 */
    String operation() default "";
    /** 详细级别 */
    AuditLevel level() default AuditLevel.NORMAL;

    enum AuditLevel {
        MINIMAL, NORMAL, DETAILED
    }
}
