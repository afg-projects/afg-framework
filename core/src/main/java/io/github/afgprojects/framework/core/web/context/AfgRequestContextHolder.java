package io.github.afgprojects.framework.core.web.context;

import org.jspecify.annotations.Nullable;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * AFG 请求上下文持有者
 * 基于 Spring RequestContextHolder 存储 RequestContext
 */
public final class AfgRequestContextHolder {

    private static final String CONTEXT_KEY = AfgRequestContextHolder.class.getName() + ".CONTEXT";

    private AfgRequestContextHolder() {}

    /**
     * 获取当前请求上下文
     *
     * @return 当前请求上下文，如果不存在则返回 null
     */
    public static @Nullable RequestContext getContext() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        return (RequestContext) attrs.getAttribute(CONTEXT_KEY, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * 设置请求上下文
     *
     * @param context 请求上下文
     * @throws IllegalStateException 如果当前线程没有绑定请求
     */
    public static void setContext(RequestContext context) {
        RequestAttributes attrs = RequestContextHolder.currentRequestAttributes();
        attrs.setAttribute(CONTEXT_KEY, context, RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * 清除请求上下文
     */
    public static void clear() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.removeAttribute(CONTEXT_KEY, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * 获取 TraceId
     */
    public static @Nullable String getTraceId() {
        RequestContext context = getContext();
        return context != null ? context.getTraceId() : null;
    }

    /**
     * 获取 RequestId
     */
    public static @Nullable String getRequestId() {
        RequestContext context = getContext();
        return context != null ? context.getRequestId() : null;
    }

    /**
     * 获取当前用户ID
     */
    public static @Nullable Long getUserId() {
        RequestContext context = getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * 获取当前用户名
     */
    public static @Nullable String getUsername() {
        RequestContext context = getContext();
        return context != null ? context.getUsername() : null;
    }

    /**
     * 获取租户ID
     */
    public static @Nullable Long getTenantId() {
        RequestContext context = getContext();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * 获取客户端IP
     */
    public static @Nullable String getClientIp() {
        RequestContext context = getContext();
        return context != null ? context.getClientIp() : null;
    }
}
