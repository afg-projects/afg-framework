package io.github.afgprojects.framework.data.core.entity;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

/**
 * 基础实体类
 * <p>
 * 所有实体的基类，包含通用字段：主键ID、创建时间、更新时间
 *
 * @param <ID> 主键类型
 */
public abstract class BaseEntity<ID> {

    /**
     * 主键ID
     */
    protected @Nullable ID id;

    /**
     * 创建时间
     */
    protected @Nullable LocalDateTime createTime;

    /**
     * 更新时间
     */
    protected @Nullable LocalDateTime updateTime;

    /**
     * 获取主键ID
     *
     * @return 主键ID
     */
    public @Nullable ID getId() {
        return id;
    }

    /**
     * 设置主键ID
     *
     * @param id 主键ID
     */
    public void setId(@Nullable ID id) {
        this.id = id;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public @Nullable LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置创建时间
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(@Nullable LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 获取更新时间
     *
     * @return 更新时间
     */
    public @Nullable LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置更新时间
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(@Nullable LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 判断是否为新建实体（未持久化）
     *
     * @return true表示新建实体
     */
    public boolean isNew() {
        return id == null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + '}';
    }
}