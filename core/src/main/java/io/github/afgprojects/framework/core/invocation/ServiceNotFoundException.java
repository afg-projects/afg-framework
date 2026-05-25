package io.github.afgprojects.framework.core.invocation;

public class ServiceNotFoundException extends ServiceInvocationException {
    private final String serviceName;
    private final String operationName;
    public ServiceNotFoundException(String serviceName) {
        super("Service not found: " + serviceName);
        this.serviceName = serviceName;
        this.operationName = null;
    }
    public ServiceNotFoundException(String serviceName, String operationName) {
        super("Service operation not found: " + serviceName + "." + operationName);
        this.serviceName = serviceName;
        this.operationName = operationName;
    }
    public String serviceName() { return serviceName; }
    public String operationName() { return operationName; }
}
