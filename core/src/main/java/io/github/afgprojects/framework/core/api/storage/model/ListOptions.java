package io.github.afgprojects.framework.core.api.storage.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 列表查询选项
 *
 * @param prefix    对象键前缀
 * @param delimiter 分隔符（用于模拟目录结构）
 * @param maxKeys   最大返回数量
 * @param marker    分页标记（从指定 key 开始）
 */
public record ListOptions(
        @Nullable String prefix,
        @Nullable String delimiter,
        int maxKeys,
        @Nullable String marker
) {

    /**
     * 默认最大返回数量
     */
    private static final int DEFAULT_MAX_KEYS = 1000;

    /**
     * 创建默认选项
     */
    @NonNull
    public static ListOptions defaults() {
        return new ListOptions(null, null, DEFAULT_MAX_KEYS, null);
    }

    /**
     * 创建带前缀的选项
     */
    @NonNull
    public static ListOptions withPrefix(@Nullable String prefix) {
        return new ListOptions(prefix, null, DEFAULT_MAX_KEYS, null);
    }

    /**
     * 创建带前缀和分隔符的选项（模拟目录）
     */
    @NonNull
    public static ListOptions withPrefixAndDelimiter(@Nullable String prefix, @NonNull String delimiter) {
        return new ListOptions(prefix, delimiter, DEFAULT_MAX_KEYS, null);
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
        private String prefix;
        private String delimiter;
        private int maxKeys = DEFAULT_MAX_KEYS;
        private String marker;

        public Builder prefix(@Nullable String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder delimiter(@Nullable String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Builder maxKeys(int maxKeys) {
            this.maxKeys = maxKeys;
            return this;
        }

        public Builder marker(@Nullable String marker) {
            this.marker = marker;
            return this;
        }

        @NonNull
        public ListOptions build() {
            return new ListOptions(prefix, delimiter, maxKeys, marker);
        }
    }
}
