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
 * 软删除接口（布尔型），表示实体使用布尔字段标记删除状态。
 *
 * <p>实现此接口的实体在"删除"时不会从数据库中物理删除，
 * 而是将 {@code deleted} 字段设置为 {@code true}。
 *
 * <p>提供 default 方法 {@link #markDeleted()} 和 {@link #markNotDeleted()}，
 * 允许实体自由组合多个特征接口（如同时实现 {@link Versioned} 和 {@link SoftDeletable}），
 * 而不依赖于具体的基类。
 *
 * @see TimestampSoftDeletable
 * @see SoftDeleteEntity
 */
@SuppressWarnings("PMD.BooleanGetMethodName")
public interface SoftDeletable {

    /**
     * 获取删除标记
     *
     * @return 是否已删除
     */
    Boolean getDeleted();

    /**
     * 设置删除标记
     *
     * @param deleted 是否已删除
     */
    void setDeleted(Boolean deleted);

    /**
     * 标记实体为已删除
     */
    default void markDeleted() {
        setDeleted(true);
    }

    /**
     * 标记实体为未删除（恢复）
     */
    default void markNotDeleted() {
        setDeleted(false);
    }

    /**
     * 判断实体是否已删除
     *
     * @return 是否已删除
     */
    default boolean isDeleted() {
        return Boolean.TRUE.equals(getDeleted());
    }
}
