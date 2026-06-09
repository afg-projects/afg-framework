package io.github.afgprojects.framework.core.invocation;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jspecify.annotations.Nullable;
import org.springframework.core.task.TaskDecorator;

import io.github.afgprojects.framework.core.context.CompositeContextTaskDecorator;
import io.github.afgprojects.framework.core.context.ThreadLocalContextPropagator;

/**
 * 调用上下文任务装饰器。
 * <p>
 * 委托给 {@link CompositeContextTaskDecorator} 实现真正的上下文传播。
 * 同时保留 {@link ThreadFactory} 接口以维持向后兼容。
 * <p>
 * 当未提供 {@link ThreadLocalContextPropagator} 时，降级为无传播的直接执行。
 *
 * @see CompositeContextTaskDecorator
 * @see ThreadLocalContextPropagator
 */
public class InvocationContextTaskDecorator implements TaskDecorator, ThreadFactory {

    private final @Nullable CompositeContextTaskDecorator delegate;
    private final ThreadFactory defaultThreadFactory = Executors.defaultThreadFactory();

    /**
     * 创建调用上下文任务装饰器（无传播，降级模式）。
     *
     * @deprecated 推荐使用 {@link #InvocationContextTaskDecorator(ThreadLocalContextPropagator)} 确保上下文传播
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public InvocationContextTaskDecorator() {
        this.delegate = null;
    }

    /**
     * 创建调用上下文任务装饰器，使用 ThreadLocalContextPropagator 实现上下文传播。
     *
     * @param propagator ThreadLocal 上下文传播器
     */
    public InvocationContextTaskDecorator(ThreadLocalContextPropagator propagator) {
        this.delegate = new CompositeContextTaskDecorator(propagator);
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        if (delegate != null) {
            return delegate.decorate(runnable);
        }
        return runnable;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Runnable decorated = decorate(runnable);
        Thread thread = defaultThreadFactory.newThread(decorated);
        thread.setName("afg-invocation-" + thread.getId());
        return thread;
    }
}
