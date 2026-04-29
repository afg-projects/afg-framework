package io.github.afgprojects.framework.data.core.relation;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * 关联元数据
 * <p>
 * 描述实体之间关联关系的元数据，包含关联类型、目标实体、外键等信息。
 */
public interface RelationMetadata {

    /**
     * 获取关联类型
     *
     * @return 关联类型
     */
    @NonNull RelationType getRelationType();

    /**
     * 获取当前实体类
     *
     * @return 当前实体类
     */
    @NonNull Class<?> getEntityClass();

    /**
     * 获取目标实体类
     *
     * @return 目标实体类
     */
    @NonNull Class<?> getTargetEntityClass();

    /**
     * 获取关联字段名
     * <p>
     * 当前实体中声明关联关系的字段名
     *
     * @return 字段名
     */
    @NonNull String getFieldName();

    /**
     * 获取映射字段名
     * <p>
     * 当关联由对方维护时，返回对方实体中关联当前实体的字段名
     *
     * @return 映射字段名，如果当前方维护关系则返回 null
     */
    @Nullable String getMappedBy();

    /**
     * 获取外键列名
     *
     * @return 外键列名
     */
    @NonNull String getForeignKeyColumn();

    /**
     * 获取中间表名
     * <p>
     * 仅对多对多关联有效
     *
     * @return 中间表名，非多对多关联返回 null
     */
    @Nullable String getJoinTable();

    /**
     * 获取中间表中当前实体的外键列名
     * <p>
     * 仅对多对多关联有效
     *
     * @return 当前实体的外键列名
     */
    @Nullable String getJoinColumn();

    /**
     * 获取中间表中目标实体的外键列名
     * <p>
     * 仅对多对多关联有效
     *
     * @return 目标实体的外键列名
     */
    @Nullable String getInverseJoinColumn();

    /**
     * 获取级联操作类型
     *
     * @return 级联类型集合
     */
    @NonNull Set<CascadeType> getCascadeTypes();

    /**
     * 获取抓取策略
     *
     * @return 抓取策略
     */
    @NonNull FetchType getFetchType();

    /**
     * 是否由当前方维护关系
     * <p>
     * 当 mappedBy 为空时，当前方负责维护外键关系
     *
     * @return 是否由当前方维护
     */
    boolean isOwningSide();

    /**
     * 是否支持级联删除孤儿实体
     * <p>
     * 仅对一对多关联有效
     *
     * @return 是否级联删除孤儿实体
     */
    boolean isOrphanRemoval();

    /**
     * 关联是否可选
     * <p>
     * 仅对多对一关联有效
     *
     * @return 关联是否可选
     */
    boolean isOptional();
}
