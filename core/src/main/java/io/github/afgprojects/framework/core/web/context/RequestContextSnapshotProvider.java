package io.github.afgprojects.framework.core.web.context;

import java.util.Map;

import io.github.afgprojects.framework.core.context.ContextSnapshotProvider;

/**
 * {@link AfgRequestContextHolder} 的上下文快照提供者。
 * <p>
 * 支持 {@link RequestContext} 在异步任务中的跨线程传播。
 * <p>
 * 注意：{@link AfgRequestContextHolder} 基于 Spring {@link org.springframework.web.context.request.RequestContextHolder}，
 * 在异步线程中通常没有请求绑定，因此快照提供者直接管理 {@link RequestContext} 对象的传播，
 * 而不依赖 {@link org.springframework.web.context.request.RequestContextHolder} 的请求属性存储。
 *
 * @see AfgRequestContextHolder
 * @see RequestContext
 */
public class RequestContextSnapshotProvider implements ContextSnapshotProvider {

    static final String KEY = "requestContext";

    /**
     * 用于异步场景的 RequestContext 存储。
     * <p>
     * 在请求线程中，RequestContext 存储在 Spring RequestContextHolder 中；
     * 在异步线程中，通过此 ThreadLocal 传播。
     */
    private static final ThreadLocal<RequestContext> ASYNC_CONTEXT = new ThreadLocal<>();

    /**
     * 在异步线程中获取 RequestContext。
     * <p>
     * 优先从 Spring RequestContextHolder 获取（如果有请求绑定），
     * 然后回退到异步传播的 ThreadLocal。
     *
     * @return RequestContext，可能为 null
     */
    public static RequestContext getAsyncContext() {
        RequestContext fromRequest = AfgRequestContextHolder.getContext();
        if (fromRequest != null) {
            return fromRequest;
        }
        return ASYNC_CONTEXT.get();
    }

    @Override
    public void capture(Map<String, Object> snapshot) {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null) {
            snapshot.put(KEY, context);
        }
    }

    @Override
    public void restore(Map<String, Object> snapshot) {
        Object value = snapshot.get(KEY);
        if (value instanceof RequestContext context) {
            // 在异步线程中无法使用 RequestContextHolder（没有请求绑定），
            // 使用专用 ThreadLocal 存储
            ASYNC_CONTEXT.set(context);
        } else {
            ASYNC_CONTEXT.remove();
        }
    }

    @Override
    public void clear() {
        ASYNC_CONTEXT.remove();
    }
}