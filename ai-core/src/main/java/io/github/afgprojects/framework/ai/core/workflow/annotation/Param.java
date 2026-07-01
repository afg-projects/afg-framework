package io.github.afgprojects.framework.ai.core.workflow.annotation;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a workflow node parameter on a record component of a node's
 * strongly-typed params record.
 *
 * <p>The abstract node base class reflects over the params record's components
 * and this annotation to build the {@link io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamSchema},
 * so the parameter is declared in exactly one place — the record — and the
 * schema, validation, and execution all read from the same source of truth.</p>
 *
 * <p>When {@link #type()} is omitted it is inferred from the record component
 * type: {@code String}/{@code CharSequence} → STRING, numeric → NUMBER,
 * {@code boolean}/{@code Boolean} → BOOLEAN, arrays / {@code Collection} → ARRAY,
 * everything else → OBJECT. {@link ParamType#ENUM} must be stated explicitly
 * together with {@link #enumValues()}.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Param {

    String displayName() default "";

    String description() default "";

    boolean required() default false;

    /**
     * Default value used when the parameter is absent at runtime.
     *
     * <p>Only meaningful for non-collection primitives / strings; for complex
     * defaults declare them in the record component's compact constructor.</p>
     */
    String defaultValue() default "";

    /**
     * Explicit parameter type. Leave default {@link ParamType#OBJECT} to trigger
     * inference from the component type (see class javadoc), except for ENUM
     * which must always be stated explicitly.
     */
    ParamType type() default ParamType.OBJECT;

    /**
     * For ENUM-typed parameters, the allowed values. Ignored otherwise.
     */
    String[] enumValues() default {};
}
