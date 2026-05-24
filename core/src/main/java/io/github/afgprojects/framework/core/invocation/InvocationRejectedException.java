package io.github.afgprojects.framework.core.invocation;

public class InvocationRejectedException extends ServiceInvocationException {
    private final String interceptorName;
    private final String reason;
    public InvocationRejectedException(String interceptorName, String reason) {
        super("Invocation rejected by " + interceptorName + ": " + reason);
        this.interceptorName = interceptorName;
        this.reason = reason;
    }
    public String interceptorName() { return interceptorName; }
    public String reason() { return reason; }
}
