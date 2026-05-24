package io.github.afgprojects.framework.core.invocation;

import java.util.List;

public interface OperationMetadata {
    String name();
    String description();
    MethodKey method();
    List<ParameterMetadata> parameters();
    String returnType();
    String returnDescription();
    String permission();
    List<String> requiredRoles();
    boolean audit();
    boolean tenantScope();
    boolean dataScope();
    boolean async();
    boolean deprecated();
    String inputSchema();
    boolean paged();
}
