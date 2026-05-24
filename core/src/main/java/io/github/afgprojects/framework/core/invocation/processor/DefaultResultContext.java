package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;

import java.util.Map;

public record DefaultResultContext(
    OperationMetadata operationMetadata,
    ServiceMetadata<?> serviceMetadata,
    ObjectMapper objectMapper,
    Map<String, Object> attributes
) implements ResultContext {}
