package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import io.github.afgprojects.framework.core.invocation.ServiceMetadata;

import java.util.Map;

public interface ResultContext {
    OperationMetadata operationMetadata();
    ServiceMetadata<?> serviceMetadata();
    ObjectMapper objectMapper();
    Map<String, Object> attributes();
}
