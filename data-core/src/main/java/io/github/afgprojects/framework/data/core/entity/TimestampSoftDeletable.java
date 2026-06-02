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

import java.time.Instant;

/**
 * 时间戳软删除接口，表示实体使用时间戳字段标记删除状态。
 *
 * <p>实现此接口的实体在"删除"时不会从数据库中物理删除，
 * 而是将 {@code deletedAt} 字段设置为删除时间，{@code null} 表示未删除。
 *
 * <p>提供 default 方法 {@link #markDeleted()} 和 {@link #markNotDeleted()}，
 * 允许实体自由组合多个特征接口。
 *
 * @see SoftDeletable
 * @see TimestampSoftDeleteEntity
 */
public interface TimestampSoftDeletable {

    /**
     * 获取删除时间
     *
     * @return 删除时间（UTC），null 表示未删除
     */
    Instant getDeletedAt();

    /**
     * 设置删除时间
     *
     * @param deletedAt 删除时间（UTC）
     */
    void setDeletedAt(Instant deletedAt);

    /**
     * 标记实体为已删除，设置删除时间为当前时间
     */
    default void markDeleted() {
        setDeletedAt(Instant.now());
    }

    /**
     * 标记实体为未删除（恢复），将删除时间设为 null
     */
    default void markNotDeleted() {
        setDeletedAt(null);
    }

    /**
     * 判断实体是否已删除
     *
     * @return 是否已删除
     */
    default boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
