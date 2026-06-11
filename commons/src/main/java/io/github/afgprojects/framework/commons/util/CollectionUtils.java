package io.github.afgprojects.framework.commons.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 集合工具类。
 * <p>提供常用集合判空、取值、分区操作。
 *
 * <p>使用示例：
 * <pre>{@code
 * CollectionUtils.isEmpty(list)              // → true
 * CollectionUtils.isNotEmpty(list)            // → false
 * CollectionUtils.first(list)                 // → 第一个元素
 * CollectionUtils.last(list)                  // → 最后一个元素
 * CollectionUtils.partition(list, 3)          // → 每组最多 3 个元素的子列表
 * }</pre>
 */
public final class CollectionUtils {

    private CollectionUtils() {
        // 工具类禁止实例化
    }

    /**
     * 判断集合是否为 null 或空
     *
     * @param collection 集合
     * @return true 如果集合为 null 或空
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否不为 null 且不为空
     *
     * @param collection 集合
     * @return true 如果集合不为 null 且不为空
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为 null 或空
     *
     * @param map Map
     * @return true 如果 Map 为 null 或空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否不为 null 且不为空
     *
     * @param map Map
     * @return true 如果 Map 不为 null 且不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 获取集合的第一个元素
     *
     * @param list 列表
     * @return 第一个元素，如果列表为空则返回 null
     */
    public static <T> T first(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * 获取集合的最后一个元素
     *
     * @param list 列表
     * @return 最后一个元素，如果列表为空则返回 null
     */
    public static <T> T last(List<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    /**
     * 将列表按指定大小分区
     *
     * @param list     原始列表
     * @param batchSize 每个分区的最大大小
     * @return 分区后的列表
     */
    public static <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (isEmpty(list) || batchSize <= 0) {
            return Collections.emptyList();
        }
        int size = list.size();
        List<List<T>> result = new java.util.ArrayList<>(size / batchSize + 1);
        for (int i = 0; i < size; i += batchSize) {
            result.add(list.subList(i, Math.min(i + batchSize, size)));
        }
        return result;
    }
}