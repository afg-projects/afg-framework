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
package io.github.afgprojects.framework.data.core.metadata;

/**
 * 实体特征枚举，用于 {@link EntityMetadata#hasTrait(EntityTrait)} 判断实体是否具有某种特征。
 *
 * <p>相比在 EntityMetadata 中硬编码 {@code isSoftDeletable()} 等方法，
 * 使用枚举特征遵循开闭原则（OCP），新增特征只需添加枚举值，
 * 无需修改 EntityMetadata 接口和所有实现类。
 */
public enum EntityTrait {

    /**
     * 软删除特征（布尔型 deleted 字段）
     *
     * @see io.github.afgprojects.framework.data.core.entity.SoftDeletable
     */
    SOFT_DELETABLE,

    /**
     * 时间戳软删除特征（LocalDateTime deletedAt 字段）
     *
     * @see io.github.afgprojects.framework.data.core.entity.TimestampSoftDeletable
     */
    TIMESTAMP_SOFT_DELETABLE,

    /**
     * 多租户特征（tenantId 字段）
     */
    TENANT_AWARE,

    /**
     * 乐观锁特征（version 字段）
     *
     * @see io.github.afgprojects.framework.data.core.entity.Versioned
     */
    VERSIONED,

    /**
     * 审计特征（createBy/updateBy 字段）
     */
    AUDITABLE,

    /**
     * 数据权限感知特征（支持按部门/组织等维度过滤数据访问）
     */
    DATA_SCOPE_AWARE,

    /**
     * 时间戳特征（createdAt/updatedAt 字段）
     */
    TIMESTAMPED
}
