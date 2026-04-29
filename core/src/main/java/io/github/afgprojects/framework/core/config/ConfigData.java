package io.github.afgprojects.framework.core.config;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 配置数据
 * 从远程配置中心获取的配置数据结构
 */
public record ConfigData(
        @NonNull String prefix,
        @NonNull Map<String, Object> properties,
        @NonNull Instant fetchedAt,
        @Nullable String source,
        @Nullable String version) {

    /**
     * 创建配置数据
     */
    public ConfigData {
        if (properties == null) {
            properties = Collections.emptyMap();
        }
        if (fetchedAt == null) {
            fetchedAt = Instant.now();
        }
    }

    /**
     * 创建简单的配置数据
     */
    public static ConfigData of(@NonNull String prefix, @NonNull Map<String, Object> properties) {
        return new ConfigData(prefix, properties, Instant.now(), null, null);
    }

    /**
     * 创建带来源的配置数据
     */
    public static ConfigData of(
            @NonNull String prefix, @NonNull Map<String, Object> properties, @Nullable String source) {
        return new ConfigData(prefix, properties, Instant.now(), source, null);
    }

    /**
     * 获取属性值
     */
    @SuppressWarnings("unchecked")
    public @Nullable <T> T getProperty(@NonNull String key) {
        return (T) properties.get(key);
    }

    /**
     * 获取属性值，带默认值
     */
    @SuppressWarnings("unchecked")
    public @NonNull <T> T getProperty(@NonNull String key, @NonNull T defaultValue) {
        Object value = properties.get(key);
        if (value == null) {
            return defaultValue;
        }
        return (T) value;
    }

    /**
     * 判断是否包含指定属性
     */
    public boolean hasProperty(@NonNull String key) {
        return properties.containsKey(key);
    }

    /**
     * 获取属性数量
     */
    public int size() {
        return properties.size();
    }

    /**
     * 判断是否为空
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * 获取不可变的属性视图
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}
