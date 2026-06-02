package io.github.afgprojects.framework.ai.core.workflow.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 工作流注解。
 * <p>
 * 标注在方法上，自动执行指定的工作流。
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Workflow {
    /** 工作流定义 ID 或名称 */
    String value();
    /** 是否异步执行 */
    boolean async() default false;
}
