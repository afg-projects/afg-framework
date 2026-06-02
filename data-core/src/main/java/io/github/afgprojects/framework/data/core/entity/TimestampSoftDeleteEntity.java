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
 * 时间戳型软删除实体，使用 {@code deletedAt} 字段标记删除状态。
 *
 * <p>继承 {@link BaseEntity}（提供 id、createdAt、updatedAt），
 * 同时实现 {@link TimestampSoftDeletable} 接口。
 *
 * @see TimestampSoftDeletable
 * @see BaseEntity
 */
public class TimestampSoftDeleteEntity extends BaseEntity implements TimestampSoftDeletable {

    /**
     * 删除时间（UTC），null 表示未删除
     */
    protected Instant deletedAt;

    @Override
    public Instant getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
