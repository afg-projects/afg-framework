package io.github.afgprojects.framework.data.core.entity;

/**
 * 软删除策略枚举
 * <p>
 * 定义软删除的实现方式，支持全局配置选择模式。
 */
public enum SoftDeleteStrategy {

    /**
     * Boolean 模式
     * <p>
     * 使用 boolean 类型的 deleted 字段标记删除状态。
     * <ul>
     *   <li>deleted = false 表示未删除</li>
     *   <li>deleted = true 表示已删除</li>
     * </ul>
     * 适用场景：简单业务场景，不需要追溯删除时间。
     */
    BOOLEAN,

    /**
     * 时间戳模式
     * <p>
     * 使用 LocalDateTime 类型的 deletedAt 字段记录删除时间。
     * <ul>
     *   <li>deletedAt = null 表示未删除</li>
     *   <li>deletedAt != null 表示已删除</li>
     * </ul>
     * 适用场景：需要追溯删除时间、按时间范围查询已删除数据、自动清理过期数据。
     */
    TIMESTAMP
}
