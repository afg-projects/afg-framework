package io.github.afgprojects.framework.data.core.entity;

/**
 * 软删除标记接口
 * <p>
 * 实现此接口的实体支持软删除功能
 */
public interface SoftDeletable {

    /**
     * 获取删除标记
     *
     * @return true表示已删除
     */
    boolean isDeleted();

    /**
     * 设置删除标记
     *
     * @param deleted 删除标记
     */
    void setDeleted(boolean deleted);
}