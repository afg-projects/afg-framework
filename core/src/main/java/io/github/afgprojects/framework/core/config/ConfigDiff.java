package io.github.afgprojects.framework.core.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * 配置差异
 * 记录配置变更的具体差异
 */
public record ConfigDiff(
        Set<String> addedKeys,
        Set<String> removedKeys,
        Set<String> changedKeys,
        Map<String, ValueChange> valueChanges) {

    /**
     * 值变更记录
     */
    public record ValueChange(@Nullable Object oldValue, @Nullable Object newValue) {
        /**
         * 判断值是否真的发生了变化
         */
        public boolean hasActualChange() {
            if (oldValue == null && newValue == null) {
                return false;
            }
            if (oldValue == null || newValue == null) {
                return true;
            }
            return !oldValue.equals(newValue);
        }
    }

    /**
     * 创建空的差异
     */
    public static ConfigDiff empty() {
        return new ConfigDiff(
                Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * 创建新增差异
     */
    public static ConfigDiff addition(Object newValue) {
        if (newValue instanceof Map<?, ?> map) {
            Set<String> addedKeys = new HashSet<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    addedKeys.add(entry.getKey().toString());
                }
            }
            return new ConfigDiff(addedKeys, Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
        }
        return new ConfigDiff(Set.of("value"), Collections.emptySet(), Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * 创建删除差异
     */
    public static ConfigDiff removal(Object oldValue) {
        if (oldValue instanceof Map<?, ?> map) {
            Set<String> removedKeys = new HashSet<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    removedKeys.add(entry.getKey().toString());
                }
            }
            return new ConfigDiff(Collections.emptySet(), removedKeys, Collections.emptySet(), Collections.emptyMap());
        }
        return new ConfigDiff(Collections.emptySet(), Set.of("value"), Collections.emptySet(), Collections.emptyMap());
    }

    /**
     * 计算两个配置之间的差异
     */
    @SuppressWarnings("unchecked")
    public static ConfigDiff compute(@Nullable Object oldValue, @Nullable Object newValue) {
        Set<String> addedKeys = new HashSet<>();
        Set<String> removedKeys = new HashSet<>();
        Set<String> changedKeys = new HashSet<>();
        Map<String, ValueChange> valueChanges = new HashMap<>();

        // 处理 Map 类型
        Map<String, Object> oldMap = toMap(oldValue);
        Map<String, Object> newMap = toMap(newValue);

        // 找出新增的 key
        for (String key : newMap.keySet()) {
            if (!oldMap.containsKey(key)) {
                addedKeys.add(key);
                valueChanges.put(key, new ValueChange(null, newMap.get(key)));
            }
        }

        // 找出删除的 key
        for (String key : oldMap.keySet()) {
            if (!newMap.containsKey(key)) {
                removedKeys.add(key);
                valueChanges.put(key, new ValueChange(oldMap.get(key), null));
            }
        }

        // 找出变更的 key
        for (String key : newMap.keySet()) {
            if (oldMap.containsKey(key)) {
                Object oldVal = oldMap.get(key);
                Object newVal = newMap.get(key);
                if (!equals(oldVal, newVal)) {
                    changedKeys.add(key);
                    valueChanges.put(key, new ValueChange(oldVal, newVal));
                }
            }
        }

        return new ConfigDiff(
                Collections.unmodifiableSet(addedKeys),
                Collections.unmodifiableSet(removedKeys),
                Collections.unmodifiableSet(changedKeys),
                Collections.unmodifiableMap(valueChanges));
    }

    /**
     * 判断是否有任何变化
     */
    public boolean isEmpty() {
        return addedKeys.isEmpty() && removedKeys.isEmpty() && changedKeys.isEmpty();
    }

    /**
     * 获取变更的总数量
     */
    public int totalChanges() {
        return addedKeys.size() + removedKeys.size() + changedKeys.size();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(@Nullable Object value) {
        if (value == null) {
            return Collections.emptyMap();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    result.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return result;
        }
        return Map.of("value", value);
    }

    private static boolean equals(@Nullable Object a, @Nullable Object b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
