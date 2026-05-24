package io.github.afgprojects.framework.core.invocation;

public interface InvocationInterceptor {
    int order();
    boolean before(InvocationContext context);
    Object after(InvocationContext context, Object result);
    void onError(InvocationContext context, Exception exception);
}
