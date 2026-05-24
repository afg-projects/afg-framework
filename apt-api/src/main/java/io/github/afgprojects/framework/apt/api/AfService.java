package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Spring Bean as a dynamically invocable service.
 * APT will extract metadata from this class at compile time.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface AfService {
    /** Service name. Defaults to class name with lowercase first letter. */
    String name() default "";
    /** Service description for documentation and AI tool definitions. */
    String description() default "";
    /** Service category (e.g. "system", "business"). */
    String category() default "";
    /** Tags for filtering and search. */
    String[] tags() default {};
    /** Whether this service is deprecated. */
    boolean deprecated() default false;
}
