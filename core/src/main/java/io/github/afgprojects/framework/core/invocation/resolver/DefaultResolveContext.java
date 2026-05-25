package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;

public record DefaultResolveContext(
        ParameterMetadata parameterMetadata,
        ObjectMapper objectMapper,
        Object rawValue
) implements ResolveContext {
}