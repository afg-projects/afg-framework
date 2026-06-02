package io.github.afgprojects.framework.ai.core.model.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.Nullable;

/**
 * AI 模型路由注解。
 * <p>
 * 标注在方法或类上，指定目标模型和备选模型。
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelRoute {
    /** 目标模型名称 */
    String value();
    /** 备选模型名称 */
    @Nullable String fallback() default "";
}
