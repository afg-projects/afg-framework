package io.github.afgprojects.framework.core.invocation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.core.invocation.InvocationContext;

public record DefaultResultContext(
        InvocationContext invocationContext,
        Object result,
        ObjectMapper objectMapper
) implements ResultContext {
}