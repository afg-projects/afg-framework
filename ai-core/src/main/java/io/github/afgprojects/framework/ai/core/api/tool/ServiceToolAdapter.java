package io.github.afgprojects.framework.ai.core.api.tool;

import io.github.afgprojects.framework.core.invocation.BeanInvocationEngine;
import io.github.afgprojects.framework.core.invocation.OperationMetadata;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts an {@link OperationMetadata} to the {@link SecureTool} interface.
 *
 * <p>Bridges the {@link BeanInvocationEngine} to the AI Tool system,
 * allowing service operations to be automatically exposed as secure tools.
 *
 * @since 1.0.0
 */
public class ServiceToolAdapter implements SecureTool<Map<String, Object>, Object> {

    private final String serviceName;
    private final OperationMetadata operation;
    private final BeanInvocationEngine engine;

    public ServiceToolAdapter(String serviceName, OperationMetadata operation, BeanInvocationEngine engine) {
        this.serviceName = serviceName;
        this.operation = operation;
        this.engine = engine;
    }

    @Override
    public String name() {
        return serviceName + "." + operation.name();
    }

    @Override
    public String description() {
        return operation.description();
    }

    @Override
    public String inputSchema() {
        return operation.inputSchema();
    }

    @Override
    public Object execute(Map<String, Object> input) {
        return engine.invoke(serviceName, operation.name(), input);
    }

    @Override
    public Object execute(Map<String, Object> input, @NonNull ToolContext context) {
        return engine.invoke(serviceName, operation.name(), input);
    }

    @Override
    public @Nullable String requiredPermission() {
        return operation.permission();
    }

    @Override
    public @NonNull Set<String> requiredRoles() {
        List<String> roles = operation.requiredRoles();
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public boolean isAuditable() {
        return operation.audit();
    }

    @Override
    public boolean isSensitive() {
        return operation.dataScope();
    }

    /**
     * Returns the service name this adapter is bound to.
     *
     * @return the service name
     */
    public String serviceName() {
        return serviceName;
    }

    /**
     * Returns the underlying operation metadata.
     *
     * @return the operation metadata
     */
    public OperationMetadata operationMetadata() {
        return operation;
    }
}
