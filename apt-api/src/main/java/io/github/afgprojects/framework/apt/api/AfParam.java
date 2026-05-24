package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for a method parameter in an @AfOperation method.
 * Used to override parameter name (when -parameters is not enabled)
 * and provide additional documentation.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.SOURCE)
public @interface AfParam {
    /** Parameter name override. Defaults to the compiled parameter name. */
    String name() default "";
    /** Parameter description for documentation and AI tool definitions. */
    String description() default "";
    /** Whether this parameter is required. Default true. */
    boolean required() default true;
    /** Default value as a string. Empty means no default. */
    String defaultValue() default "";
    /** Enum values hint for AI tool definitions. */
    String[] enumValues() default {};
}
