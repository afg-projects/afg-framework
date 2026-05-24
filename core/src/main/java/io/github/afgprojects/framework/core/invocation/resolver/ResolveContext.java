package io.github.afgprojects.framework.core.invocation.resolver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.ParameterMetadata;
import java.util.Map;

public interface ResolveContext {
    ParameterMetadata parameterMetadata();
    ObjectMapper objectMapper();
    Map<String, Object> rawArguments();
}
