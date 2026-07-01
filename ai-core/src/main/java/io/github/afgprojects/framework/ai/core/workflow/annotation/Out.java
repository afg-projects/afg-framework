package io.github.afgprojects.framework.ai.core.workflow.annotation;

import io.github.afgprojects.framework.ai.core.api.workflow.definition.ParamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a workflow node output field on a record component of a node's
 * output-descriptor record.
 *
 * <p>Paired with {@link Param} on the params record, this lets a node fully
 * describe its own {@link io.github.afgprojects.framework.ai.core.api.workflow.definition.NodeDefinition}
 * (inputs and outputs) from record metadata alone.</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.RECORD_COMPONENT)
public @interface Out {

    String displayName() default "";

    String description() default "";

    boolean optional() default false;

    /**
     * Explicit output type; defaults to {@link ParamType#OBJECT} which triggers
     * inference from the component type (same rules as {@link Param#type()}).
     */
    ParamType type() default ParamType.OBJECT;
}
