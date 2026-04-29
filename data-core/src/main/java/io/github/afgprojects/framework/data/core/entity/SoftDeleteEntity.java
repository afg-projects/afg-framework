package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;

/**
 * 软删除实体类
 * <p>
 * 支持软删除的实体，继承 BaseEntity，增加 deleted 字段
 *
 * @param <ID> 主键类型
 */
public abstract class SoftDeleteEntity<ID> extends BaseEntity<ID> implements SoftDeletable {

    /**
     * 删除标记（0-未删除，1-已删除）
     */
    protected boolean deleted = false;

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * 标记为已删除
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * 恢复删除
     */
    public void restore() {
        this.deleted = false;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", deleted=" + deleted + '}';
    }
}