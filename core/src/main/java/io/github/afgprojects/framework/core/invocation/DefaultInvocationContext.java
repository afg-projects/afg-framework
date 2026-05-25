package io.github.afgprojects.framework.core.invocation;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class DefaultInvocationContext implements InvocationContext {

    private final String serviceName;
    private final String operationName;
    private final ServiceMetadata<?> serviceMetadata;
    private final OperationMetadata operationMetadata;
    private final @Nullable Map<String, Object> arguments;
    private final Object[] resolvedArguments;
    private final Object bean;
    private final Method method;
    private final Map<String, Object> attributes = new HashMap<>();

    public DefaultInvocationContext(String serviceName,
                                    String operationName,
                                    ServiceMetadata<?> serviceMetadata,
                                    OperationMetadata operationMetadata,
                                    @Nullable Map<String, Object> arguments,
                                    Object[] resolvedArguments,
                                    Object bean,
                                    Method method) {
        this.serviceName = serviceName;
        this.operationName = operationName;
        this.serviceMetadata = serviceMetadata;
        this.operationMetadata = operationMetadata;
        this.arguments = arguments;
        this.resolvedArguments = resolvedArguments;
        this.bean = bean;
        this.method = method;
    }

    @Override public String serviceName() { return serviceName; }
    @Override public String operationName() { return operationName; }
    @Override public ServiceMetadata<?> serviceMetadata() { return serviceMetadata; }
    @Override public OperationMetadata operationMetadata() { return operationMetadata; }
    @Override public @Nullable Map<String, Object> arguments() { return arguments; }
    @Override public Object[] resolvedArguments() { return resolvedArguments; }
    @Override public Object bean() { return bean; }
    @Override public Method method() { return method; }
    @Override public Map<String, Object> attributes() { return attributes; }
}