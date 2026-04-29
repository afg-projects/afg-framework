package io.github.afgprojects.framework.core.config;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

/**
 * 配置变更事件
 * 包含配置变更的完整信息
 */
public record ConfigChangeEvent(
        String prefix,
        @Nullable Object oldValue,
        @Nullable Object newValue,
        ConfigDiff diff,
        Instant changedAt,
        ConfigSource source) {

    /**
     * 创建配置变更事件
     */
    public ConfigChangeEvent {
        if (changedAt == null) {
            changedAt = Instant.now();
        }
    }

    /**
     * 判断是否有变化
     */
    public boolean hasChanges() {
        return !diff.isEmpty();
    }

    /**
     * 判断是否为新增配置
     */
    public boolean isAddition() {
        return oldValue == null && newValue != null;
    }

    /**
     * 判断是否为删除配置
     */
    public boolean isRemoval() {
        return oldValue != null && newValue == null;
    }

    /**
     * 判断是否为更新配置
     */
    public boolean isUpdate() {
        return oldValue != null && newValue != null;
    }

    /**
     * 创建新增事件
     */
    public static ConfigChangeEvent addition(String prefix, Object newValue, ConfigSource source) {
        return new ConfigChangeEvent(prefix, null, newValue, ConfigDiff.addition(newValue), Instant.now(), source);
    }

    /**
     * 创建删除事件
     */
    public static ConfigChangeEvent removal(String prefix, Object oldValue, ConfigSource source) {
        return new ConfigChangeEvent(prefix, oldValue, null, ConfigDiff.removal(oldValue), Instant.now(), source);
    }

    /**
     * 创建更新事件
     */
    public static ConfigChangeEvent update(String prefix, Object oldValue, Object newValue, ConfigSource source) {
        return new ConfigChangeEvent(
                prefix, oldValue, newValue, ConfigDiff.compute(oldValue, newValue), Instant.now(), source);
    }
}