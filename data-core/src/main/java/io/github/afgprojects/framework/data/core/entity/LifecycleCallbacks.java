/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.afgprojects.framework.data.core.entity;

import java.util.function.Consumer;

/**
 * 实体生命周期回调接口，类似 JPA 的 {@code @PrePersist}、{@code @PreUpdate}、
 * {@code @PostLoad} 等回调。
 *
 * <p>实体可以实现此接口，在保存和加载时执行自定义逻辑。
 * 框架会在适当的时机自动调用这些回调方法。
 *
 * <p>此接口的所有方法都提供了 default 空实现，实体可以选择性覆盖需要的方法。
 */
public interface LifecycleCallbacks {

    /**
     * 实体持久化之前的回调（新建时）。
     *
     * <p>类似 JPA 的 {@code @PrePersist}。
     * 通常用于设置创建时间、创建人等字段。
     */
    default void beforeCreate() {
        // 默认空实现
    }

    /**
     * 实体更新之前的回调。
     *
     * <p>类似 JPA 的 {@code @PreUpdate}。
     * 通常用于设置更新时间、更新人等字段。
     */
    default void beforeUpdate() {
        // 默认空实现
    }

    /**
     * 实体从数据库加载之后的回调。
     *
     * <p>类似 JPA 的 {@code @PostLoad}。
     * 通常用于初始化瞬态字段、执行数据转换等。
     */
    default void afterLoad() {
        // 默认空实现
    }

    /**
     * 实体删除之前的回调。
     *
     * <p>类似 JPA 的 {@code @PreRemove}。
     * 通常用于执行级联校验、清理关联资源等。
     */
    default void beforeDelete() {
        // 默认空实现
    }

    /**
     * 实体持久化之后的回调（新建时）。
     *
     * <p>类似 JPA 的 {@code @PostPersist}。
     * 在 INSERT 成功后、事务提交前调用。
     * 通常用于执行创建后的业务逻辑，如发送通知、初始化关联数据等。
     *
     * <p>注意：如果需要事务提交后触发，应使用 {@code EntityChangedEvent} 机制。
     *
     * @param entity 持久化后的实体（包含生成的主键）
     */
    default void afterCreate(Object entity) {
        // 默认空实现
    }

    /**
     * 实体更新之后的回调。
     *
     * <p>类似 JPA 的 {@code @PostUpdate}。
     * 在 UPDATE 成功后、事务提交前调用。
     * 通常用于执行更新后的业务逻辑，如同步缓存、触发级联更新等。
     *
     * <p>注意：如果需要事务提交后触发，应使用 {@code EntityChangedEvent} 机制。
     *
     * @param entity 更新后的实体
     */
    default void afterUpdate(Object entity) {
        // 默认空实现
    }

    /**
     * 实体删除之后的回调。
     *
     * <p>类似 JPA 的 {@code @PostRemove}。
     * 在 DELETE 成功后、事务提交前调用。
     * 通常用于执行删除后的业务逻辑，如清理关联资源、归档数据等。
     *
     * <p>注意：如果需要事务提交后触发，应使用 {@code EntityChangedEvent} 机制。
     *
     * @param entity 被删除的实体
     */
    default void afterDelete(Object entity) {
        // 默认空实现
    }

    /**
     * 如果实体实现了 {@link LifecycleCallbacks} 接口，则执行给定的回调操作。
     *
     * <p>此方法提供了一种类型安全的方式来触发生命周期回调，
     * 避免在业务代码中重复 {@code instanceof} 检查和类型转换。
     *
     * <p>使用示例：
     * <pre>{@code
     * LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::beforeCreate);
     * LifecycleCallbacks.ifCallback(entity, LifecycleCallbacks::afterLoad);
     * }</pre>
     *
     * @param entity   实体对象，可能为 null
     * @param action   当实体实现 LifecycleCallbacks 时要执行的操作
     * @param <T>      实体类型
     */
    static <T> void ifCallback(T entity, Consumer<LifecycleCallbacks> action) {
        if (entity instanceof LifecycleCallbacks callbacks) {
            action.accept(callbacks);
        }
    }
}
