package io.github.afgprojects.framework.core.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ThreadLocal 上下文传播器注册表。
 * <p>
 * 统一管理所有 ContextHolder 的异步传播逻辑。
 * 通过注册 {@link ContextSnapshotProvider} 实例，可以在异步任务执行时
 * 自动捕获和恢复所有已注册的 ThreadLocal 上下文。
 *
 * <h3>工作原理</h3>
 * <ol>
 *   <li>启动时：各 ContextHolder 的 {@link ContextSnapshotProvider} 通过 AutoConfiguration 注册</li>
 *   <li>异步任务提交时：{@link #captureAll()} 捕获当前线程所有已注册上下文的快照</li>
 *   <li>异步任务执行前：{@link #restoreAll(Map)} 将快照恢复到工作线程</li>
 *   <li>异步任务执行后：{@link #clearAll()} 清除工作线程的上下文，防止泄漏</li>
 * </ol>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 注册提供者
 * propagator.register(dataScopeProvider);
 * propagator.register(baggageProvider);
 *
 * // 在 TaskDecorator 中使用
 * Map<String, Object> snapshot = propagator.captureAll();
 * executor.submit(() -> {
 *     try {
 *         propagator.restoreAll(snapshot);
 *         // 执行业务逻辑
 *     } finally {
 *         propagator.clearAll();
 *     }
 * });
 * }</pre>
 *
 * @see ContextSnapshotProvider
 * @see CompositeContextTaskDecorator
 */
public class ThreadLocalContextPropagator {

    private final List<ContextSnapshotProvider> providers = new CopyOnWriteArrayList<>();

    /**
     * 注册一个上下文快照提供者。
     *
     * @param provider 上下文快照提供者
     */
    public void register(ContextSnapshotProvider provider) {
        providers.add(provider);
    }

    /**
     * 捕获当前线程所有已注册上下文的快照。
     *
     * @return 上下文快照 Map，key 由各 provider 自行定义
     */
    public Map<String, Object> captureAll() {
        Map<String, Object> snapshot = new HashMap<>();
        for (ContextSnapshotProvider provider : providers) {
            provider.capture(snapshot);
        }
        return snapshot;
    }

    /**
     * 将快照恢复到当前线程。
     *
     * @param snapshot 上下文快照 Map
     */
    public void restoreAll(Map<String, Object> snapshot) {
        for (ContextSnapshotProvider provider : providers) {
            provider.restore(snapshot);
        }
    }

    /**
     * 清除当前线程所有已注册上下文。
     * <p>
     * 在异步任务执行完成后调用，防止线程池复用时的上下文泄漏。
     */
    public void clearAll() {
        for (ContextSnapshotProvider provider : providers) {
            provider.clear();
        }
    }

    /**
     * 获取已注册的提供者数量。
     *
     * @return 提供者数量
     */
    public int getProviderCount() {
        return providers.size();
    }
}
