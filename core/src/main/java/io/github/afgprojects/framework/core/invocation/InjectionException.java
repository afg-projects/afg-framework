package io.github.afgprojects.framework.core.invocation;

public class InjectionException extends ServiceInvocationException {
    private final String paramName;
    private final Class<?> paramType;
    public InjectionException(String paramName, Class<?> paramType) {
        super("Cannot inject parameter '" + paramName + "' of type " + paramType.getName());
        this.paramName = paramName;
        this.paramType = paramType;
    }
}
