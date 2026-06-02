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
 * 全功能实体，包含所有常用特征：软删除、乐观锁、审计字段、多租户。
 *
 * <p>继承 {@link BaseEntity}（提供 id、createdAt、updatedAt），
 * 同时实现 {@link SoftDeletable}、{@link Versioned}、{@link Auditable} 接口。
 *
 * <p>对于不需要全部特征的场景，可以：
 * <ul>
 *   <li>直接继承 {@link BaseEntity} 并选择实现需要的特征接口</li>
 *   <li>继承 {@link SoftDeleteEntity}、{@link VersionedEntity} 等部分特征基类</li>
 * </ul>
 *
 * @see BaseEntity
 * @see SoftDeletable
 * @see Versioned
 * @see Auditable
 */
public class FullEntity extends BaseEntity implements SoftDeletable, Versioned, Auditable {

    /**
     * 删除标记
     */
    protected Boolean deleted = false;

    /**
     * 版本号
     */
    protected Integer version = 0;

    /**
     * 创建人
     */
    protected String createBy;

    /**
     * 修改人
     */
    protected String updateBy;

    // --- SoftDeletable ---

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    // --- Versioned ---

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    // --- 审计字段 ---

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }
}
