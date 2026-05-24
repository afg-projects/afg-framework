package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provides metadata for the return value of an @AfOperation method.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AfResult {
    /** Return value description. */
    String description() default "";
    /** Whether the result is a paged list. */
    boolean paged() default false;
    /** Whether the result is a streaming response. */
    boolean streaming() default false;
}
