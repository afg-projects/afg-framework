package io.github.afgprojects.framework.core.invocation.resolver;

import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public interface ArgumentResolver {

    int priority();

    /**
     * Checks if this resolver can handle converting the raw value for the given parameter.
     *
     * @param param    the parameter metadata describing the target type
     * @param rawValue the raw value to be converted
     * @return true if this resolver can handle the conversion
     */
    boolean supports(ParameterMetadata param, Object rawValue);

    /**
     * Resolves the raw value to the parameter's target type.
     *
     * @param context the resolve context
     * @return the converted value
     */
    Object resolve(ResolveContext context);
}