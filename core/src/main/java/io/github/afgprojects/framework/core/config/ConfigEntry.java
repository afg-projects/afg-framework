package io.github.afgprojects.framework.core.config;

import java.util.Objects;

import lombok.Builder;

/**
 * 配置条目
 * 记录配置来源、值和加载时间
 */
@Builder
public record ConfigEntry(ConfigSource source, String prefix, Object value, long loadedAt) {

    public ConfigEntry {
        if (source == null) {
            throw new IllegalArgumentException("Source cannot be null");
        }
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix cannot be null or blank");
        }
        if (loadedAt == 0) {
            loadedAt = System.currentTimeMillis();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigEntry that = (ConfigEntry) o;
        return Objects.equals(prefix, that.prefix) && source == that.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, source);
    }

    @Override
    public String toString() {
        return "ConfigEntry{" + "source=" + source + ", prefix='" + prefix + '\'' + ", loadedAt=" + loadedAt + '}';
    }

    public static ConfigEntryBuilder builder() {
        return new ConfigEntryBuilder();
    }

    public static class ConfigEntryBuilder {
        private ConfigSource source;
        private String prefix;
        private Object value;
        private long loadedAt;

        public ConfigEntryBuilder source(ConfigSource source) {
            this.source = source;
            return this;
        }

        public ConfigEntryBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public ConfigEntryBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public ConfigEntryBuilder loadedAt(long loadedAt) {
            this.loadedAt = loadedAt;
            return this;
        }

        public ConfigEntryBuilder loadedAtNow() {
            this.loadedAt = System.currentTimeMillis();
            return this;
        }

        public ConfigEntry build() {
            if (loadedAt == 0) {
                this.loadedAt = System.currentTimeMillis();
            }
            return new ConfigEntry(source, prefix, value, loadedAt);
        }
    }
}
