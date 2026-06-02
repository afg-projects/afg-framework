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
 * 乐观锁实体，包含版本号字段用于乐观锁控制。
 *
 * <p>继承 {@link BaseEntity}（提供 id、createdAt、updatedAt），
 * 同时实现 {@link Versioned} 接口。
 *
 * <p>如果需要同时拥有乐观锁和软删除特征，可以直接继承此类并实现
 * {@link SoftDeletable} 或 {@link TimestampSoftDeletable}，或使用 {@link FullEntity}。
 *
 * @see Versioned
 * @see BaseEntity
 */
public class VersionedEntity extends BaseEntity implements Versioned {

    /**
     * 版本号
     */
    protected Integer version = 0;

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }
}
