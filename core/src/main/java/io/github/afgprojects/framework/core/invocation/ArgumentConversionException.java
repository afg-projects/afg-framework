package io.github.afgprojects.framework.core.invocation;

public class ArgumentConversionException extends ServiceInvocationException {
    private final String paramName;
    private final Class<?> sourceType;
    private final Class<?> targetType;
    public ArgumentConversionException(String paramName, Class<?> sourceType, Class<?> targetType, Throwable cause) {
        super("Cannot convert argument '" + paramName + "' from " + sourceType.getName() + " to " + targetType.getName(), cause);
        this.paramName = paramName;
        this.sourceType = sourceType;
        this.targetType = targetType;
    }
}
