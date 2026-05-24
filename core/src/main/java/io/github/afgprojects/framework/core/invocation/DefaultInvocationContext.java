package io.github.afgprojects.framework.core.invocation;

import java.util.Map;

public record DefaultInvocationContext(
    ServiceMetadata<?> serviceMetadata,
    OperationMetadata operationMetadata,
    Object targetBean,
    Object[] arguments,
    Map<String, Object> rawArguments,
    Map<String, Object> attributes
) implements InvocationContext {}
