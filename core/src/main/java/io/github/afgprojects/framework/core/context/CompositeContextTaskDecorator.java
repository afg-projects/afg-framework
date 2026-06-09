package io.github.afgprojects.framework.core.context;

import java.util.Map;

import org.springframework.core.task.TaskDecorator;

/**
 * 基于 {@link ThreadLocalContextPropagator} 的组合 TaskDecorator。
 * <p>
 * 在异步任务执行前恢复上下文，执行后清除，防止线程池复用时的上下文泄漏。
 * <p>
 * 替代了 {@link io.github.afgprojects.framework.core.invocation.InvocationContextTaskDecorator}
 * 中的空操作 {@link InvocationContextTaskDecorator.ContextSnapshot}，实现了真正的上下文传播。
 *
 * <h3>工作流程</h3>
 * <pre>
 * 主线程: captureAll() -> snapshot
 * 异步线程: restoreAll(snapshot) -> runnable.run() -> clearAll()
 * </pre>
 *
 * @see ThreadLocalContextPropagator
 * @see ContextSnapshotProvider
 */
public class CompositeContextTaskDecorator implements TaskDecorator {

    private final ThreadLocalContextPropagator propagator;

    /**
     * 创建组合上下文 TaskDecorator。
     *
     * @param propagator ThreadLocal 上下文传播器
     */
    public CompositeContextTaskDecorator(ThreadLocalContextPropagator propagator) {
        this.propagator = propagator;
    }

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, Object> snapshot = propagator.captureAll();
        return () -> {
            try {
                propagator.restoreAll(snapshot);
                runnable.run();
            } finally {
                propagator.clearAll();
            }
        };
    }
}