package io.github.afgprojects.framework.data.core.config;

import io.github.afgprojects.framework.data.core.entity.SoftDeleteStrategy;
import org.jspecify.annotations.Nullable;

/**
 * 软删除配置
 * <p>
 * 用于配置软删除行为，支持全局配置选择模式。
 */
public class SoftDeleteConfig {

    /**
     * 默认软删除策略
     */
    public static final SoftDeleteStrategy DEFAULT_STRATEGY = SoftDeleteStrategy.BOOLEAN;

    /**
     * 默认已删除值（Boolean 模式）
     */
    public static final boolean DEFAULT_DELETED_VALUE = true;

    /**
     * 默认未删除值（Boolean 模式）
     */
    public static final boolean DEFAULT_NOT_DELETED_VALUE = false;

    /**
     * 是否启用软删除
     */
    private boolean enabled = true;

    /**
     * 软删除策略
     */
    private SoftDeleteStrategy strategy = DEFAULT_STRATEGY;

    /**
     * Boolean 模式下的已删除值字段名
     */
    private String booleanFieldName = "deleted";

    /**
     * 时间戳模式下的删除时间字段名
     */
    private String timestampFieldName = "deletedAt";

    /**
     * 是否在查询时自动过滤已删除记录
     */
    private boolean autoFilterDeleted = true;

    /**
     * 创建默认配置
     *
     * @return 默认配置
     */
    public static SoftDeleteConfig defaults() {
        return new SoftDeleteConfig();
    }

    /**
     * 创建 Boolean 模式配置
     *
     * @return Boolean 模式配置
     */
    public static SoftDeleteConfig booleanStrategy() {
        SoftDeleteConfig config = new SoftDeleteConfig();
        config.setStrategy(SoftDeleteStrategy.BOOLEAN);
        return config;
    }

    /**
     * 创建时间戳模式配置
     *
     * @return 时间戳模式配置
     */
    public static SoftDeleteConfig timestampStrategy() {
        SoftDeleteConfig config = new SoftDeleteConfig();
        config.setStrategy(SoftDeleteStrategy.TIMESTAMP);
        return config;
    }

    // ==================== Getters & Setters ====================

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public SoftDeleteStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(SoftDeleteStrategy strategy) {
        this.strategy = strategy;
    }

    public String getBooleanFieldName() {
        return booleanFieldName;
    }

    public void setBooleanFieldName(String booleanFieldName) {
        this.booleanFieldName = booleanFieldName;
    }

    public String getTimestampFieldName() {
        return timestampFieldName;
    }

    public void setTimestampFieldName(String timestampFieldName) {
        this.timestampFieldName = timestampFieldName;
    }

    public boolean isAutoFilterDeleted() {
        return autoFilterDeleted;
    }

    public void setAutoFilterDeleted(boolean autoFilterDeleted) {
        this.autoFilterDeleted = autoFilterDeleted;
    }

    /**
     * 根据策略获取软删除字段名
     *
     * @return 字段名
     */
    public String getFieldName() {
        return strategy == SoftDeleteStrategy.TIMESTAMP ? timestampFieldName : booleanFieldName;
    }

    @Override
    public String toString() {
        return "SoftDeleteConfig{" +
            "enabled=" + enabled +
            ", strategy=" + strategy +
            ", fieldName='" + getFieldName() + '\'' +
            ", autoFilterDeleted=" + autoFilterDeleted +
            '}';
    }
}
