package io.github.afgprojects.framework.data.core.entity;

import io.github.afgprojects.framework.data.core.context.EntityContext;

/**
 * 生命周期回调接口
 * <p>
 * 实现此接口的实体在 CRUD 操作前后会收到回调通知
 */
public interface LifecycleCallbacks {

    /**
     * 创建前回调（事务内）
     */
    default void beforeCreate(EntityContext context) {}

    /**
     * 创建后回调（事务内，数据库操作已完成但事务未提交）
     */
    default void afterCreate(EntityContext context) {}

    /**
     * 更新前回调（事务内）
     */
    default void beforeUpdate(EntityContext context) {}

    /**
     * 更新后回调（事务内）
     */
    default void afterUpdate(EntityContext context) {}

    /**
     * 删除前回调（事务内）
     */
    default void beforeDelete(EntityContext context) {}

    /**
     * 删除后回调（事务内）
     */
    default void afterDelete(EntityContext context) {}
}
