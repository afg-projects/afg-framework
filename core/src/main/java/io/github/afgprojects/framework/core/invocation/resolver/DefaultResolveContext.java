package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import java.util.Map;

public record DefaultResolveContext(
    ParameterMetadata parameterMetadata,
    ObjectMapper objectMapper,
    Map<String, Object> rawArguments
) implements ResolveContext {}
