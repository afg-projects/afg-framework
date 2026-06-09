package io.github.afgprojects.framework.core.security.datascope;

import java.util.Map;

import io.github.afgprojects.framework.core.context.ContextSnapshotProvider;

/**
 * {@link DataScopeContextHolder} 的上下文快照提供者。
 * <p>
 * 支持 {@link DataScopeContext} 在异步任务中的跨线程传播。
 *
 * @see DataScopeContextHolder
 * @see DataScopeContext
 */
public class DataScopeContextSnapshotProvider implements ContextSnapshotProvider {

    static final String KEY = "dataScope";

    @Override
    public void capture(Map<String, Object> snapshot) {
        DataScopeContext context = DataScopeContextHolder.getContext();
        if (context != null) {
            snapshot.put(KEY, context);
        }
    }

    @Override
    public void restore(Map<String, Object> snapshot) {
        Object value = snapshot.get(KEY);
        if (value instanceof DataScopeContext context) {
            DataScopeContextHolder.setContext(context);
        } else {
            DataScopeContextHolder.clear();
        }
    }

    @Override
    public void clear() {
        DataScopeContextHolder.clear();
    }
}
