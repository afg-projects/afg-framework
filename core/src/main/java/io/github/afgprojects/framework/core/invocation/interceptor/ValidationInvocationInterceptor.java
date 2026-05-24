package io.github.afgprojects.framework.core.invocation.interceptor;

import io.github.afgprojects.framework.core.invocation.InvocationContext;
import io.github.afgprojects.framework.core.invocation.InvocationInterceptor;
import io.github.afgprojects.framework.core.invocation.ServiceInvocationException;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import java.util.Set;
import java.util.stream.Collectors;

public class ValidationInvocationInterceptor implements InvocationInterceptor {
    private final Validator validator;

    public ValidationInvocationInterceptor(Validator validator) { this.validator = validator; }
    public ValidationInvocationInterceptor() { this.validator = null; }

    @Override public int order() { return 500; }

    @Override
    public boolean before(InvocationContext context) {
        if (validator == null) return true;
        for (Object arg : context.arguments()) {
            if (arg == null) continue;
            Set<ConstraintViolation<Object>> violations = validator.validate(arg);
            if (!violations.isEmpty()) {
                String msg = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new ServiceInvocationException("Validation failed: " + msg);
            }
        }
        return true;
    }

    @Override public Object after(InvocationContext context, Object result) { return result; }
    @Override public void onError(InvocationContext context, Exception exception) {}
}
