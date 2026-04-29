package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 时间戳软删除实体类
 * <p>
 * 支持时间戳软删除的实体，继承 BaseEntity，使用 deletedAt 字段记录删除时间。
 * <ul>
 *   <li>deletedAt = null 表示未删除</li>
 *   <li>deletedAt != null 表示已删除，值为删除时间</li>
 * </ul>
 * <p>
 * 相比 boolean 类型的软删除，时间戳模式有以下优势：
 * <ul>
 *   <li>可追溯删除时间</li>
 *   <li>支持按删除时间范围查询</li>
 *   <li>支持自动清理过期已删除数据</li>
 * </ul>
 *
 * @param <ID> 主键类型
 */
public abstract class TimestampSoftDeleteEntity<ID> extends BaseEntity<ID> implements TimestampSoftDeletable {

    /**
     * 删除时间（null 表示未删除）
     */
    protected @Nullable LocalDateTime deletedAt;

    @Override
    public boolean isDeleted() {
        return deletedAt != null;
    }

    @Override
    public @Nullable LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(@Nullable LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 标记为已删除
     * <p>
     * 设置当前时间为删除时间
     */
    public void markDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 标记为已删除，使用指定时间
     *
     * @param deletedAt 删除时间
     */
    public void markDeleted(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    /**
     * 恢复删除
     * <p>
     * 将删除时间设置为 null
     */
    public void restore() {
        this.deletedAt = null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", deletedAt=" + deletedAt + '}';
    }
}
