package io.github.afgprojects.framework.core.invocation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Map;

public interface InvocationContext {

    String serviceName();

    String operationName();

    ServiceMetadata<?> serviceMetadata();

    OperationMetadata operationMetadata();

    /**
     * Returns the raw named arguments, or null if positional arguments were used.
     */
    @Nullable Map<String, Object> arguments();

    Object[] resolvedArguments();

    Object bean();

    Method method();

    Map<String, Object> attributes();
}