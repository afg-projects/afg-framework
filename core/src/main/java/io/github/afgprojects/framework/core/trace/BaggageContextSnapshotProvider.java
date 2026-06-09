package io.github.afgprojects.framework.core.trace;

import java.util.HashMap;
import java.util.Map;

import io.github.afgprojects.framework.core.context.ContextSnapshotProvider;

/**
 * {@link BaggageContext} 的上下文快照提供者。
 * <p>
 * 支持 Baggage 数据在异步任务中的跨线程传播。
 * 使用基于 ThreadLocal 的本地存储而非 ConcurrentHashMap，修复了原始实现中的并发正确性问题。
 *
 * @see BaggageContext
 */
public class BaggageContextSnapshotProvider implements ContextSnapshotProvider {

    static final String KEY = "baggage";

    @Override
    public void capture(Map<String, Object> snapshot) {
        Map<String, String> baggage = BaggageContext.getLocalBaggage();
        if (!baggage.isEmpty()) {
            snapshot.put(KEY, new HashMap<>(baggage));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restore(Map<String, Object> snapshot) {
        Object value = snapshot.get(KEY);
        if (value instanceof Map<?, ?> map) {
            Map<String, String> baggage = (Map<String, String>) map;
            BaggageContext.restoreLocalBaggage(baggage);
        } else {
            BaggageContext.clearLocalBaggage();
        }
    }

    @Override
    public void clear() {
        BaggageContext.clearLocalBaggage();
    }
}