package io.github.afgprojects.framework.core.api.config;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 配置变更事件
 *
 * @param key        配置键
 * @param group      配置分组
 * @param oldValue   旧值
 * @param newValue   新值
 * @param changeType 变更类型
 */
public record ConfigChangeEvent(
        @NonNull String key,
        @NonNull String group,
        @Nullable String oldValue,
        @Nullable String newValue,
        @NonNull ConfigChangeType changeType) {

    /**
     * 配置变更类型
     */
    public enum ConfigChangeType {
        /** 新增配置 */
        ADDED,
        /** 修改配置 */
        MODIFIED,
        /** 删除配置 */
        DELETED
    }

    /**
     * 判断是否为新增配置
     *
     * @return 如果是新增配置返回 true
     */
    public boolean isAddition() {
        return changeType == ConfigChangeType.ADDED;
    }

    /**
     * 判断是否为修改配置
     *
     * @return 如果是修改配置返回 true
     */
    public boolean isModification() {
        return changeType == ConfigChangeType.MODIFIED;
    }

    /**
     * 判断是否为删除配置
     *
     * @return 如果是删除配置返回 true
     */
    public boolean isDeletion() {
        return changeType == ConfigChangeType.DELETED;
    }
}
