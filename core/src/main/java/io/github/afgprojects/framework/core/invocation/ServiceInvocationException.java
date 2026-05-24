package io.github.afgprojects.framework.core.invocation;

public class ServiceInvocationException extends RuntimeException {
    public ServiceInvocationException(String message) { super(message); }
    public ServiceInvocationException(String message, Throwable cause) { super(message, cause); }
}
