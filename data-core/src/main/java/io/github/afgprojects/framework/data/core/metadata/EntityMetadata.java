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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.github.afgprojects.framework.data.core.entity.EncryptedFieldMetadata;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;

/**
 * 实体元数据接口，描述实体的结构信息。
 *
 * <p>提供实体类、表名、字段、关联关系、软删除、多租户等元信息。
 * 使用 {@link #hasTrait(EntityTrait)} 判断实体是否具有某种特征，遵循开闭原则（OCP），
 * 新增特征只需添加枚举值，无需修改此接口。
 */
public interface EntityMetadata<T> {

    /**
     * 获取实体类
     *
     * @return 实体类
     */
    Class<T> getEntityClass();

    /**
     * 获取表名
     *
     * @return 表名
     */
    String getTableName();

    /**
     * 获取所有字段元数据
     *
     * @return 字段元数据列表
     */
    List<? extends FieldMetadata> getFields();

    /**
     * 根据字段名获取字段元数据
     *
     * @param fieldName 字段名
     * @return 字段元数据，不存在返回 null
     */
    FieldMetadata getField(String fieldName);

    /**
     * 获取主键字段元数据
     *
     * @return 主键字段元数据
     */
    FieldMetadata getIdField();

    /**
     * 获取主键字段名
     *
     * @return 主键字段名
     */
    String getIdFieldName();

    /**
     * 获取软删除字段元数据
     *
     * @return 软删除字段元数据，不存在返回 null
     */
    FieldMetadata getSoftDeleteField();

    /**
     * 获取租户字段元数据
     *
     * @return 租户字段元数据，不存在返回 null
     */
    FieldMetadata getTenantField();

    /**
     * 获取列名到字段名的映射
     *
     * @return 列名到字段名的映射
     */
    Map<String, String> getColumnToFieldMap();

    /**
     * 获取字段名到列名的映射
     *
     * @return 字段名到列名的映射
     */
    Map<String, String> getFieldToColumnMap();

    /**
     * 判断实体是否具有指定特征。
     *
     * <p>遵循开闭原则（OCP），新增特征只需添加 {@link EntityTrait} 枚举值，
     * 无需修改此接口和所有实现类。
     *
     * @param trait 实体特征
     * @return 是否具有该特征
     * @see EntityTrait
     */
    boolean hasTrait(EntityTrait trait);

    /**
     * 获取实体的所有特征
     *
     * @return 特征集合
     */
    Set<EntityTrait> getTraits();


    /**
     * 获取默认查询条件
     *
     * @return 默认查询条件
     */
    Condition getDefaultCondition();

    /**
     * 判断实体是否具有指定字段的关联关系
     *
     * @param fieldName 字段名
     * @return 是否具有关联关系
     */
    boolean hasRelation(String fieldName);

    /**
     * 获取所有关联关系元数据
     *
     * @return 关联关系元数据列表
     */
    List<RelationMetadata> getRelations();

    /**
     * 判断指定字段是否为加密字段
     *
     * @param fieldName 字段名（Java 属性名）
     * @return 如果字段标注了 @EncryptedField 则返回 true
     */
    default boolean isEncrypted(String fieldName) {
        return false;
    }

    /**
     * 获取所有加密字段的元数据
     *
     * @return 加密字段元数据列表，如果没有加密字段则返回空列表
     */
    default List<EncryptedFieldMetadata> getEncryptedFields() {
        return List.of();
    }

    /**
     * 根据字段名获取关联关系元数据
     *
     * @param fieldName 字段名
     * @return 关联关系元数据
     */
    default Optional<RelationMetadata> getRelation(String fieldName) {
        return getRelations().stream()
            .filter(r -> r.getFieldName().equals(fieldName))
            .findFirst();
    }
}
