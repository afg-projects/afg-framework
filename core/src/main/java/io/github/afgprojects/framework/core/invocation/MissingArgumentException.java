package io.github.afgprojects.framework.core.invocation;

public class MissingArgumentException extends ServiceInvocationException {
    private final String paramName;
    public MissingArgumentException(String paramName) {
        super("Required argument missing: " + paramName);
        this.paramName = paramName;
    }
    public String paramName() { return paramName; }
}
