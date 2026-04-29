package io.github.afgprojects.framework.core.metrics;

import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

/**
 * 指标标签提供者接口
 *
 * <p>提供动态标签的能力
 *
 * <h3>使用示例</h3>
 * <pre>
 * public class TenantTagProvider implements MetricsTagProvider {
 *     @Override
 *     public Iterable&lt;Tag&gt; getTags() {
 *         return Tags.of("tenant", TenantContext.getCurrentTenant());
 *     }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface MetricsTagProvider {

    /**
     * 提供标签
     *
     * @return 标签集合
     */
    @NonNull Iterable<Tag> getTags();

    /**
     * 从 Map 创建标签提供者
     *
     * @param tags 标签映射
     * @return 标签提供者
     */
    @NonNull
    static MetricsTagProvider fromMap(@Nullable Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections::emptyList;
        }
        return () -> Tags.of(tags.entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .toList());
    }

    /**
     * 从键值对创建标签提供者
     *
     * @param key   标签键
     * @param value 标签值
     * @return 标签提供者
     */
    @NonNull
    static MetricsTagProvider of(@NonNull String key, @NonNull String value) {
        return () -> Tags.of(key, value);
    }

    /**
     * 从多个键值对创建标签提供者
     *
     * @param keyValues 键值对（key1, value1, key2, value2...）
     * @return 标签提供者
     */
    @NonNull
    static MetricsTagProvider of(@NonNull String... keyValues) {
        return () -> Tags.of(keyValues);
    }
}