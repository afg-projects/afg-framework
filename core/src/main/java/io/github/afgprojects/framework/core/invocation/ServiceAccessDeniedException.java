package io.github.afgprojects.framework.core.invocation;

public class ServiceAccessDeniedException extends ServiceInvocationException {
    private final String permission;
    public ServiceAccessDeniedException(String permission) {
        super("Access denied: missing permission '" + permission + "'");
        this.permission = permission;
    }
    public String permission() { return permission; }
}
