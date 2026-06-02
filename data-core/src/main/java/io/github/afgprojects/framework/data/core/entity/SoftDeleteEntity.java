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
 * 布尔型软删除实体，使用 {@code deleted} 字段标记删除状态。
 *
 * <p>继承 {@link BaseEntity}（提供 id、createdAt、updatedAt），
 * 同时实现 {@link SoftDeletable} 接口。
 *
 * <p>如果需要同时拥有软删除和乐观锁特征，可以直接继承 {@link VersionedEntity}
 * 并实现 {@link SoftDeletable}，或使用 {@link FullEntity}。
 *
 * @see SoftDeletable
 * @see BaseEntity
 */
public class SoftDeleteEntity extends BaseEntity implements SoftDeletable {

    /**
     * 删除标记
     */
    protected Boolean deleted = false;

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
