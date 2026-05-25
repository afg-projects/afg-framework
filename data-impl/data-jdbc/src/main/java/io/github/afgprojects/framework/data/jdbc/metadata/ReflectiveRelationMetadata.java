package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.commons.naming.NamingUtils;
import io.github.afgprojects.framework.data.core.relation.CascadeType;
import io.github.afgprojects.framework.data.core.relation.FetchType;
import io.github.afgprojects.framework.data.core.relation.ManyToMany;
import io.github.afgprojects.framework.data.core.relation.ManyToOne;
import io.github.afgprojects.framework.data.core.relation.OneToMany;
import io.github.afgprojects.framework.data.core.relation.OneToOne;
import io.github.afgprojects.framework.commons.naming.NamingUtils;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 基于反射的关联元数据实现
 * <p>
 * 通过反射从实体类关联字段提取元数据，支持框架自定义关联注解。
 */
public class ReflectiveRelationMetadata implements RelationMetadata {

    private final RelationType relationType;
    private final Class<?> entityClass;
    private final Class<?> targetEntityClass;
    private final String fieldName;
    private final String foreignKeyColumn;
    private final String mappedBy;
    private final String joinTable;
    private final String joinColumn;
    private final String inverseJoinColumn;
    private final Set<CascadeType> cascadeTypes;
    private final FetchType fetchType;
    private final boolean owningSide;
    private final boolean orphanRemoval;
    private final boolean optional;

    /**
     * 从字段创建关联元数据
     *
     * @param entityClass 实体类
     * @param field 关联字段
     */
    public ReflectiveRelationMetadata(Class<?> entityClass, Field field) {
        this.entityClass = entityClass;
        this.fieldName = field.getName();
        this.targetEntityClass = inferTargetEntity(field);
        this.foreignKeyColumn = inferForeignKeyColumn(field);
        this.mappedBy = inferMappedBy(field);
        this.joinTable = inferJoinTable(field);
        this.joinColumn = inferJoinColumn(field, entityClass);
        this.inverseJoinColumn = inferInverseJoinColumn(field, targetEntityClass);
        this.cascadeTypes = inferCascadeTypes(field);
        this.fetchType = inferFetchType(field);
        this.relationType = inferRelationType(field);
        this.owningSide = this.mappedBy == null || this.mappedBy.isEmpty();
        this.orphanRemoval = inferOrphanRemoval(field);
        this.optional = inferOptional(field);
    }

    @Override
    public @NonNull RelationType getRelationType() {
        return relationType;
    }

    @Override
    public @NonNull Class<?> getEntityClass() {
        return entityClass;
    }

    @Override
    public @NonNull Class<?> getTargetEntityClass() {
        return targetEntityClass;
    }

    @Override
    public @NonNull String getFieldName() {
        return fieldName;
    }

    @Override
    public @Nullable String getMappedBy() {
        return mappedBy;
    }

    @Override
    public @NonNull String getForeignKeyColumn() {
        return foreignKeyColumn;
    }

    @Override
    public @Nullable String getJoinTable() {
        return joinTable;
    }

    @Override
    public @Nullable String getJoinColumn() {
        return joinColumn;
    }

    @Override
    public @Nullable String getInverseJoinColumn() {
        return inverseJoinColumn;
    }

    @Override
    public @NonNull Set<CascadeType> getCascadeTypes() {
        return cascadeTypes;
    }

    @Override
    public @NonNull FetchType getFetchType() {
        return fetchType;
    }

    @Override
    public boolean isOwningSide() {
        return owningSide;
    }

    @Override
    public boolean isOrphanRemoval() {
        return orphanRemoval;
    }

    @Override
    public boolean isOptional() {
        return optional;
    }

    // ==================== 推断方法 ====================

    private static RelationType inferRelationType(Field field) {
        if (field.getAnnotation(ManyToOne.class) != null) {
            return RelationType.MANY_TO_ONE;
        }
        if (field.getAnnotation(OneToMany.class) != null) {
            return RelationType.ONE_TO_MANY;
        }
        if (field.getAnnotation(OneToOne.class) != null) {
            return RelationType.ONE_TO_ONE;
        }
        if (field.getAnnotation(ManyToMany.class) != null) {
            return RelationType.MANY_TO_MANY;
        }
        return RelationType.MANY_TO_ONE; // 默认
    }

    private static Class<?> inferTargetEntity(Field field) {
        ManyToOne mto = field.getAnnotation(ManyToOne.class);
        if (mto != null && mto.targetEntity() != void.class) {
            return mto.targetEntity();
        }
        OneToMany otm = field.getAnnotation(OneToMany.class);
        if (otm != null && otm.targetEntity() != void.class) {
            return otm.targetEntity();
        }
        OneToOne oto = field.getAnnotation(OneToOne.class);
        if (oto != null && oto.targetEntity() != void.class) {
            return oto.targetEntity();
        }
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null && mtm.targetEntity() != void.class) {
            return mtm.targetEntity();
        }
        // 尝试从泛型参数提取类型（适用于 Collection<X>, List<X>, Set<X>）
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType pt) {
            Type[] typeArgs = pt.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class) {
                return (Class<?>) typeArgs[0];
            }
        }
        // 默认使用字段类型
        return field.getType();
    }

    private static String inferForeignKeyColumn(Field field) {
        ManyToOne mto = field.getAnnotation(ManyToOne.class);
        if (mto != null && !mto.foreignKey().isEmpty()) {
            return mto.foreignKey();
        }
        OneToOne oto = field.getAnnotation(OneToOne.class);
        if (oto != null && !oto.foreignKey().isEmpty()) {
            return oto.foreignKey();
        }
        // 默认：字段名_id
        return NamingUtils.toSnakeCase(field.getName()) + "_id";
    }

    private static String inferMappedBy(Field field) {
        OneToMany otm = field.getAnnotation(OneToMany.class);
        if (otm != null && !otm.mappedBy().isEmpty()) {
            return otm.mappedBy();
        }
        OneToOne oto = field.getAnnotation(OneToOne.class);
        if (oto != null && !oto.mappedBy().isEmpty()) {
            return oto.mappedBy();
        }
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null && !mtm.mappedBy().isEmpty()) {
            return mtm.mappedBy();
        }
        return null;
    }

    private static String inferJoinTable(Field field) {
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null && !mtm.joinTable().isEmpty()) {
            return mtm.joinTable();
        }
        // 默认：实体表名_目标实体表名
        if (mtm != null) {
            return NamingUtils.toSnakeCase(field.getDeclaringClass().getSimpleName()) + "_" +
                   NamingUtils.toSnakeCase(inferTargetEntity(field).getSimpleName());
        }
        return null;
    }

    private static String inferJoinColumn(Field field, Class<?> entityClass) {
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null && !mtm.joinColumn().isEmpty()) {
            return mtm.joinColumn();
        }
        // 默认：实体表名_id
        if (mtm != null) {
            return NamingUtils.toSnakeCase(entityClass.getSimpleName()) + "_id";
        }
        return null;
    }

    private static String inferInverseJoinColumn(Field field, Class<?> targetEntityClass) {
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null && !mtm.inverseJoinColumn().isEmpty()) {
            return mtm.inverseJoinColumn();
        }
        // 默认：目标实体表名_id
        if (mtm != null) {
            return NamingUtils.toSnakeCase(targetEntityClass.getSimpleName()) + "_id";
        }
        return null;
    }

    private static Set<CascadeType> inferCascadeTypes(Field field) {
        ManyToOne mto = field.getAnnotation(ManyToOne.class);
        if (mto != null) {
            return Arrays.stream(mto.cascade()).collect(Collectors.toSet());
        }
        OneToMany otm = field.getAnnotation(OneToMany.class);
        if (otm != null) {
            return Arrays.stream(otm.cascade()).collect(Collectors.toSet());
        }
        OneToOne oto = field.getAnnotation(OneToOne.class);
        if (oto != null) {
            return Arrays.stream(oto.cascade()).collect(Collectors.toSet());
        }
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null) {
            return Arrays.stream(mtm.cascade()).collect(Collectors.toSet());
        }
        return Set.of();
    }

    private static FetchType inferFetchType(Field field) {
        ManyToOne mto = field.getAnnotation(ManyToOne.class);
        if (mto != null) {
            return mto.fetch();
        }
        OneToMany otm = field.getAnnotation(OneToMany.class);
        if (otm != null) {
            return otm.fetch();
        }
        OneToOne oto = field.getAnnotation(OneToOne.class);
        if (oto != null) {
            return oto.fetch();
        }
        ManyToMany mtm = field.getAnnotation(ManyToMany.class);
        if (mtm != null) {
            return mtm.fetch();
        }
        return FetchType.EAGER; // 默认
    }

    private static boolean inferOrphanRemoval(Field field) {
        OneToMany otm = field.getAnnotation(OneToMany.class);
        if (otm != null) {
            return otm.orphanRemoval();
        }
        return false;
    }

    private static boolean inferOptional(Field field) {
        ManyToOne mto = field.getAnnotation(ManyToOne.class);
        if (mto != null) {
            return mto.optional();
        }
        return true;
    }

}