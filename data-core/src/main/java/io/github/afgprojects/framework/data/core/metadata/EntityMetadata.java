package io.github.afgprojects.framework.data.core.metadata;

import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 实体元数据
 */
public interface EntityMetadata<T> {

    /**
     * 实体类型
     */
    Class<T> getEntityClass();

    /**
     * 表名
     */
    String getTableName();

    /**
     * 主键字段
     */
    FieldMetadata getIdField();

    /**
     * 所有字段
     */
    List<FieldMetadata> getFields();

    /**
     * 根据属性名获取字段
     */
    @Nullable FieldMetadata getField(String propertyName);

    /**
     * 是否软删除实体
     */
    boolean isSoftDeletable();

    /**
     * 是否多租户实体
     */
    boolean isTenantAware();

    /**
     * 是否审计实体
     */
    boolean isAuditable();

    /**
     * 是否版本化实体
     */
    boolean isVersioned();

    // ==================== 关联元数据 ====================

    /**
     * 获取所有关联元数据
     *
     * @return 关联元数据列表
     */
    List<RelationMetadata> getRelations();

    /**
     * 根据字段名获取关联元数据
     *
     * @param fieldName 字段名
     * @return 关联元数据，不存在则返回空
     */
    Optional<RelationMetadata> getRelation(String fieldName);

    /**
     * 是否有指定字段的关联
     *
     * @param fieldName 字段名
     * @return 是否存在关联
     */
    boolean hasRelation(String fieldName);
}
