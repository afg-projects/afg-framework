package io.github.afgprojects.framework.core.api.storage.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 文件存储元数据
 */
public class StorageMetadata {

    private final Map<String, String> data;

    public StorageMetadata() {
        this.data = new HashMap<>();
    }

    public StorageMetadata(@NonNull Map<String, String> data) {
        this.data = new HashMap<>(data);
    }

    /**
     * 获取元数据值
     */
    @Nullable
    public String get(@NonNull String key) {
        return data.get(key);
    }

    /**
     * 设置元数据值
     */
    public void put(@NonNull String key, @NonNull String value) {
        data.put(key, value);
    }

    /**
     * 获取所有元数据（不可变）
     */
    @NonNull
    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(data);
    }

    /**
     * 是否包含指定键
     */
    public boolean containsKey(@NonNull String key) {
        return data.containsKey(key);
    }

    /**
     * 元数据是否为空
     */
    public boolean isEmpty() {
        return data.isEmpty();
    }

    /**
     * 创建 Builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder 类
     */
    public static class Builder {
        private final Map<String, String> data = new HashMap<>();

        public Builder put(@NonNull String key, @NonNull String value) {
            data.put(key, value);
            return this;
        }

        public Builder putAll(@NonNull Map<String, String> map) {
            data.putAll(map);
            return this;
        }

        @NonNull
        public StorageMetadata build() {
            return new StorageMetadata(data);
        }
    }
}
