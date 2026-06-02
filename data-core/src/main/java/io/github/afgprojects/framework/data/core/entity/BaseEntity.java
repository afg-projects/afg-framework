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
 * 实体基类，提供 ID 和时间戳字段。
 *
 * <p>使用 {@link Instant} 替代 {@link java.time.LocalDateTime}，
 * 以确保在分布式系统中时间戳的一致性（Instant 包含 UTC 时区信息）。
 *
 * <p>提供了基于 ID 的 {@link #equals(Object)} 和 {@link #hashCode()} 实现，
 * 符合 JPA 实体的最佳实践：
 * <ul>
 *   <li>新建实体（id 为 null）使用对象身份判断相等性</li>
 *   <li>已持久化实体使用 id 判断相等性</li>
 * </ul>
 */
public abstract class BaseEntity {

    /**
     * 实体 ID
     */
    protected Long id;

    /**
     * 创建时间（UTC）
     */
    protected Instant createdAt;

    /**
     * 更新时间（UTC）
     */
    protected Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * 基于 ID 的相等性判断。
     *
     * <p>如果两个实体的 ID 都不为 null 且相等，则认为它们相等。
     * 如果两个实体的 ID 都为 null（新建实体），则使用对象身份（==）判断。
     * 如果一个 ID 为 null 另一个不为 null，则不相等。
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity that = (BaseEntity) obj;
        // 新建实体（id 为 null）使用对象身份
        if (this.id == null || that.id == null) return false;
        return id.equals(that.id);
    }

    /**
     * 基于 ID 的哈希码。
     *
     * <p>新建实体（id 为 null）使用 {@link System#identityHashCode(Object)}，
     * 已持久化实体使用 id 的哈希码。
     */
    @Override
    public int hashCode() {
        return id == null ? System.identityHashCode(this) : id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(id=" + id + ")";
    }
}
