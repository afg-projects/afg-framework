package io.github.afgprojects.framework.core.context;

import java.util.Map;

/**
 * 上下文快照提供者接口。
 * <p>
 * 每个 ContextHolder 实现此接口以支持异步传播。
 * {@link ThreadLocalContextPropagator} 通过此接口统一管理所有上下文的跨线程传播。
 * <p>
 * 实现类应将自身管理的 ThreadLocal 数据捕获到 snapshot Map 中，
 * 并能从 snapshot Map 恢复到当前线程的 ThreadLocal。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // DataScopeContextHolder 的适配实现
 * public class DataScopeContextSnapshotProvider implements ContextSnapshotProvider {
 *     public void capture(Map<String, Object> snapshot) {
 *         snapshot.put("dataScope", DataScopeContextHolder.getContext());
 *     }
 *     public void restore(Map<String, Object> snapshot) {
 *         DataScopeContextHolder.setContext((DataScopeContext) snapshot.get("dataScope"));
 *     }
 *     public void clear() {
 *         DataScopeContextHolder.clear();
 *     }
 * }
 * }</pre>
 *
 * @see ThreadLocalContextPropagator
 */
public interface ContextSnapshotProvider {

    /**
     * 将当前线程上下文捕获到 snapshot 中。
     * <p>
     * 实现应将自身管理的 ThreadLocal 数据写入 snapshot Map。
     * Key 应使用唯一前缀避免冲突（如 "dataScope", "baggage"）。
     *
     * @param snapshot 快照 Map，由 {@link ThreadLocalContextPropagator} 提供
     */
    void capture(Map<String, Object> snapshot);

    /**
     * 从 snapshot 恢复上下文到当前线程。
     * <p>
     * 实现应从 snapshot Map 读取之前捕获的数据，
     * 并设置到当前线程的 ThreadLocal 中。
     * 如果 snapshot 中不包含自身的数据，应清除当前线程的上下文。
     *
     * @param snapshot 快照 Map，包含之前捕获的数据
     */
    void restore(Map<String, Object> snapshot);

    /**
     * 清除当前线程上下文。
     * <p>
     * 清除当前线程中自身管理的 ThreadLocal 数据，
     * 防止在线程池复用时上下文泄漏。
     */
    void clear();
}
