package io.github.afgprojects.framework.data.jdbc.metadata;

import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.*;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 简单实体元数据实现
 */
public class SimpleEntityMetadata<T> implements EntityMetadata<T> {

    private final Class<T> entityClass;
    private final String tableName;
    private final List<FieldMetadata> fields;
    private final List<RelationMetadata> relations;
    private final Map<String, RelationMetadata> relationMap;

    public SimpleEntityMetadata(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = inferTableName(entityClass);
        this.fields = inferFields(entityClass);
        this.relations = inferRelations(entityClass);
        this.relationMap = new HashMap<>();
        for (RelationMetadata relation : relations) {
            relationMap.put(relation.getFieldName(), relation);
        }
    }

    @Override
    public Class<T> getEntityClass() {
        return entityClass;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public FieldMetadata getIdField() {
        // 优先使用 isId() 方法识别主键（支持注解）
        for (FieldMetadata field : fields) {
            if (field.isId()) {
                return field;
            }
        }
        // 后备：查找名为 id 的字段（兼容无注解场景）
        for (FieldMetadata field : fields) {
            if ("id".equals(field.getPropertyName())) {
                return field;
            }
        }
        return null;
    }

    @Override
    public List<FieldMetadata> getFields() {
        return fields;
    }

    @Override
    public @Nullable FieldMetadata getField(String propertyName) {
        for (FieldMetadata field : fields) {
            if (field.getPropertyName().equals(propertyName)) {
                return field;
            }
        }
        return null;
    }

    @Override
    public boolean isSoftDeletable() {
        // 检查是否有 deleted 或 deletedAt 字段
        return getField("deleted") != null || getField("deletedAt") != null;
    }

    @Override
    public boolean isTenantAware() {
        // 检查是否有 tenantId 字段
        return getField("tenantId") != null;
    }

    @Override
    public boolean isAuditable() {
        // 检查是否有 createdAt/updatedAt 字段
        return getField("createdAt") != null && getField("updatedAt") != null;
    }

    @Override
    public boolean isVersioned() {
        // 检查是否有 version 字段
        return getField("version") != null;
    }

    // ==================== 关联元数据 ====================

    @Override
    public List<RelationMetadata> getRelations() {
        return relations;
    }

    @Override
    public Optional<RelationMetadata> getRelation(String fieldName) {
        return Optional.ofNullable(relationMap.get(fieldName));
    }

    @Override
    public boolean hasRelation(String fieldName) {
        return relationMap.containsKey(fieldName);
    }

    /**
     * 推断实体表名
     *
     * @param entityClass 实体类
     * @return 表名（snake_case）
     */
    private String inferTableName(Class<?> entityClass) {
        String className = entityClass.getSimpleName();
        StringBuilder tableName = new StringBuilder();
        for (int i = 0; i < className.length(); i++) {
            char c = className.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                tableName.append('_');
            }
            tableName.append(Character.toLowerCase(c));
        }
        return tableName.toString();
    }

    /**
     * 推断实体字段（包括从父类继承的字段）
     * <p>
     * 排除带有关联注解的字段（@ManyToOne、@OneToMany、@OneToOne、@ManyToMany）。
     *
     * @param entityClass 实体类
     * @return 字段列表
     */
    private List<FieldMetadata> inferFields(Class<?> entityClass) {
        List<FieldMetadata> fields = new ArrayList<>();
        // 遍历类层次结构，收集所有字段（包括继承的字段）
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            for (java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                // 跳过关联字段
                if (isRelationField(field)) {
                    continue;
                }
                // 使用支持注解识别的构造方法
                fields.add(new SimpleFieldMetadata(field));
            }
            currentClass = currentClass.getSuperclass();
        }
        return fields;
    }

    /**
     * 检查字段是否为关联字段
     *
     * @param field 字段
     * @return 是否为关联字段
     */
    private boolean isRelationField(java.lang.reflect.Field field) {
        return field.isAnnotationPresent(ManyToOne.class)
                || field.isAnnotationPresent(OneToMany.class)
                || field.isAnnotationPresent(OneToOne.class)
                || field.isAnnotationPresent(ManyToMany.class);
    }

    /**
     * 推断实体的关联关系
     * <p>
     * 通过扫描实体字段上的关联注解（@ManyToOne、@OneToMany、@OneToOne、@ManyToMany）
     * 来构建关联元数据。
     *
     * @param entityClass 实体类
     * @return 关联元数据列表
     */
    private List<RelationMetadata> inferRelations(Class<T> entityClass) {
        List<RelationMetadata> relations = new ArrayList<>();
        // 遍历类层次结构，收集所有关联字段
        Class<?> currentClass = entityClass;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                RelationMetadata relation = buildRelationMetadata(entityClass, field);
                if (relation != null) {
                    relations.add(relation);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return relations;
    }

    /**
     * 根据字段上的关联注解构建关联元数据
     *
     * @param entityClass 实体类
     * @param field       字段
     * @return 关联元数据，如果字段没有关联注解则返回 null
     */
    private @Nullable RelationMetadata buildRelationMetadata(Class<T> entityClass, Field field) {
        // 检查 @ManyToOne 注解
        ManyToOne manyToOne = field.getAnnotation(ManyToOne.class);
        if (manyToOne != null) {
            return buildManyToOneMetadata(entityClass, field, manyToOne);
        }

        // 检查 @OneToMany 注解
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany != null) {
            return buildOneToManyMetadata(entityClass, field, oneToMany);
        }

        // 检查 @OneToOne 注解
        OneToOne oneToOne = field.getAnnotation(OneToOne.class);
        if (oneToOne != null) {
            return buildOneToOneMetadata(entityClass, field, oneToOne);
        }

        // 检查 @ManyToMany 注解
        ManyToMany manyToMany = field.getAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            return buildManyToManyMetadata(entityClass, field, manyToMany);
        }

        return null;
    }

    /**
     * 构建 ManyToOne 关联元数据
     */
    private RelationMetadata buildManyToOneMetadata(Class<T> entityClass, Field field, ManyToOne annotation) {
        Class<?> targetEntity = annotation.targetEntity() != void.class
                ? annotation.targetEntity()
                : field.getType();

        String foreignKey = annotation.foreignKey().isEmpty()
                ? field.getName() + "_id"
                : annotation.foreignKey();

        Set<CascadeType> cascadeTypes = arrayToSet(annotation.cascade());

        return RelationMetadataImpl.builder()
                .relationType(RelationType.MANY_TO_ONE)
                .entityClass(entityClass)
                .targetEntityClass(targetEntity)
                .fieldName(field.getName())
                .foreignKeyColumn(foreignKey)
                .cascadeTypes(cascadeTypes)
                .fetchType(annotation.fetch())
                .optional(annotation.optional())
                .build();
    }

    /**
     * 构建 OneToMany 关联元数据
     */
    private RelationMetadata buildOneToManyMetadata(Class<T> entityClass, Field field, OneToMany annotation) {
        Class<?> targetEntity = annotation.targetEntity() != void.class
                ? annotation.targetEntity()
                : getGenericFieldType(field);

        String foreignKey = annotation.foreignKey().isEmpty()
                ? inferTableName(entityClass) + "_id"
                : annotation.foreignKey();

        Set<CascadeType> cascadeTypes = arrayToSet(annotation.cascade());

        return RelationMetadataImpl.builder()
                .relationType(RelationType.ONE_TO_MANY)
                .entityClass(entityClass)
                .targetEntityClass(targetEntity)
                .fieldName(field.getName())
                .mappedBy(annotation.mappedBy())
                .foreignKeyColumn(foreignKey)
                .cascadeTypes(cascadeTypes)
                .fetchType(annotation.fetch())
                .orphanRemoval(annotation.orphanRemoval())
                .build();
    }

    /**
     * 构建 OneToOne 关联元数据
     */
    private RelationMetadata buildOneToOneMetadata(Class<T> entityClass, Field field, OneToOne annotation) {
        Class<?> targetEntity = annotation.targetEntity() != void.class
                ? annotation.targetEntity()
                : field.getType();

        String foreignKey = annotation.foreignKey().isEmpty()
                ? field.getName() + "_id"
                : annotation.foreignKey();

        Set<CascadeType> cascadeTypes = arrayToSet(annotation.cascade());

        return RelationMetadataImpl.builder()
                .relationType(RelationType.ONE_TO_ONE)
                .entityClass(entityClass)
                .targetEntityClass(targetEntity)
                .fieldName(field.getName())
                .mappedBy(annotation.mappedBy())
                .foreignKeyColumn(foreignKey)
                .cascadeTypes(cascadeTypes)
                .fetchType(annotation.fetch())
                .build();
    }

    /**
     * 构建 ManyToMany 关联元数据
     */
    private RelationMetadata buildManyToManyMetadata(Class<T> entityClass, Field field, ManyToMany annotation) {
        Class<?> targetEntity = annotation.targetEntity() != void.class
                ? annotation.targetEntity()
                : getGenericFieldType(field);

        String joinTable = annotation.joinTable().isEmpty()
                ? inferTableName(entityClass) + "_" + inferTableName(targetEntity)
                : annotation.joinTable();

        String joinColumn = annotation.joinColumn().isEmpty()
                ? inferTableName(entityClass) + "_id"
                : annotation.joinColumn();

        String inverseJoinColumn = annotation.inverseJoinColumn().isEmpty()
                ? inferTableName(targetEntity) + "_id"
                : annotation.inverseJoinColumn();

        Set<CascadeType> cascadeTypes = arrayToSet(annotation.cascade());

        return RelationMetadataImpl.builder()
                .relationType(RelationType.MANY_TO_MANY)
                .entityClass(entityClass)
                .targetEntityClass(targetEntity)
                .fieldName(field.getName())
                .mappedBy(annotation.mappedBy())
                .foreignKeyColumn("")
                .joinTable(joinTable)
                .joinColumn(joinColumn)
                .inverseJoinColumn(inverseJoinColumn)
                .cascadeTypes(cascadeTypes)
                .fetchType(annotation.fetch())
                .build();
    }

    /**
     * 获取字段的泛型类型（用于 Collection/Set/List 字段）
     *
     * @param field 字段
     * @return 泛型类型，如果不是泛型则返回字段类型
     */
    private Class<?> getGenericFieldType(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();
            if (typeArgs.length > 0 && typeArgs[0] instanceof Class<?> clazz) {
                return clazz;
            }
        }
        return field.getType();
    }

    /**
     * 将数组转换为 Set
     *
     * @param array 数组
     * @return Set
     */
    private Set<CascadeType> arrayToSet(CascadeType[] array) {
        if (array == null || array.length == 0) {
            return Collections.emptySet();
        }
        Set<CascadeType> set = EnumSet.noneOf(CascadeType.class);
        set.addAll(Arrays.asList(array));
        return set;
    }
}
