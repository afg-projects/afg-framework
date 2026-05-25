package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public interface ResolveContext {

    ParameterMetadata parameterMetadata();

    ObjectMapper objectMapper();

    /**
     * Returns the raw value to be converted.
     */
    Object rawValue();
}