package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * 时间戳软删除标记接口
 * <p>
 * 实现此接口的实体支持时间戳软删除功能。
 * 与 {@link SoftDeletable} 不同，此接口使用时间戳而非 boolean 标记删除状态。
 */
public interface TimestampSoftDeletable {

    /**
     * 判断是否已删除
     *
     * @return true 表示已删除
     */
    boolean isDeleted();

    /**
     * 获取删除时间
     *
     * @return 删除时间，null 表示未删除
     */
    @Nullable LocalDateTime getDeletedAt();

    /**
     * 设置删除时间
     *
     * @param deletedAt 删除时间，null 表示恢复
     */
    void setDeletedAt(@Nullable LocalDateTime deletedAt);
}
