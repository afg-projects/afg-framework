package io.github.afgprojects.framework.ai.core.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContentSafety {
    /** 检查输入内容 */
    boolean checkInput() default true;
    /** 检查输出内容 */
    boolean checkOutput() default true;
    /** 是否拦截不安全内容（true=抛异常，false=标记） */
    boolean block() default true;
}
