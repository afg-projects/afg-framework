package io.github.afgprojects.framework.apt.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a dynamically invocable operation.
 * Must be used within a class annotated with @AfService.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface AfOperation {
    /** Operation name. Must be unique within the same @AfService. Defaults to method name. */
    String name() default "";
    /** Operation description for documentation and AI tool definitions. */
    String description() default "";
    /** Whether this operation is inherently asynchronous. */
    boolean async() default false;
    /** Whether this operation is deprecated. */
    boolean deprecated() default false;
    /** Permission identifier required to invoke this operation. Empty means no permission check. */
    String permission() default "";
    /** Roles required to invoke this operation. Empty means no role check. */
    String[] requiredRoles() default {};
    /** Whether to audit this operation invocation. Default true. */
    boolean audit() default true;
    /** Whether this operation is scoped to the current tenant. Default true. */
    boolean tenantScope() default true;
    /** Whether data scope filtering applies to this operation. Default false. */
    boolean dataScope() default false;
    /** Manual JSON Schema for input. Empty means APT auto-derives from method signature. */
    String inputSchema() default "";
}
