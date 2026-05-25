package io.github.afgprojects.framework.core.invocation;

public class ArgumentConversionException extends ServiceInvocationException {
    private final String paramName;
    private final String targetType;
    private final String sourceType;
    public ArgumentConversionException(String paramName, String targetType, String sourceType, Throwable cause) {
        super("Cannot convert argument '" + paramName + "' from " + sourceType + " to " + targetType, cause);
        this.paramName = paramName;
        this.targetType = targetType;
        this.sourceType = sourceType;
    }
}
