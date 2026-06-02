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
}
