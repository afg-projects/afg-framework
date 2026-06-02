package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.EntityProxy;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationMetadata;
import io.github.afgprojects.framework.data.core.relation.RelationType;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 关联加载辅助类
 * <p>
 * 负责处理实体关联关系的加载逻辑，支持 ManyToOne、OneToMany、OneToOne、ManyToMany 关联类型。
 * 提供单个加载和批量加载功能。
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
class AssociationLoader {

    private final Dialect dialect;
    private final JdbcDataManager dataManager;

    /**
     * 字段缓存，避免每次反射查找（使用 ConcurrentHashMap 保证线程安全）
     */
    private final Map<Class<?>, Map<String, Field>> fieldCache = new ConcurrentHashMap<>();

    AssociationLoader(Dialect dialect, JdbcDataManager dataManager) {
        this.dialect = dialect;
        this.dataManager = dataManager;
    }

    // ==================== 单个加载 ====================

    /**
     * 执行关联数据加载
     *
     * @param entity       实体对象
     * @param idValue      实体ID
     * @param relation     关联元数据
     * @param entityClass  实体类
     * @param metadata     实体元数据
     * @return 关联数据
     */
    @SuppressWarnings("unchecked")
    Object fetchAssociation(Object entity, Object idValue, RelationMetadata relation,
                            Class<?> entityClass, EntityMetadata<?> metadata) {
        RelationType relationType = relation.getRelationType();
        Class<?> targetEntityClass = relation.getTargetEntityClass();

        return switch (relationType) {
            case MANY_TO_ONE -> fetchManyToOne(entity, relation, targetEntityClass, metadata);
            case ONE_TO_ONE -> fetchOneToOne(entity, idValue, relation, targetEntityClass, metadata);
            case ONE_TO_MANY -> fetchOneToMany(idValue, relation, targetEntityClass);
            case MANY_TO_MANY -> fetchManyToMany(idValue, relation, targetEntityClass);
        };
    }

    /**
     * 加载 ManyToOne 关联
     */
    Object fetchManyToOne(Object entity, RelationMetadata relation, Class<?> targetEntityClass,
                          EntityMetadata<?> metadata) {
        Object foreignKeyValue = getForeignKeyValue(entity, relation, metadata);
        if (foreignKeyValue == null) {
            return null;
        }

        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        return targetProxy.findById(foreignKeyValue).orElse(null);
    }

    /**
     * 加载 OneToOne 关联
     */
    Object fetchOneToOne(Object entity, Object idValue, RelationMetadata relation,
                         Class<?> targetEntityClass, EntityMetadata<?> metadata) {
        if (relation.isOwningSide()) {
            return fetchManyToOne(entity, relation, targetEntityClass, metadata);
        }

        String mappedBy = relation.getMappedBy();
        String foreignKeyColumn = mappedBy + "_id";

        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        Condition condition = Conditions.eq(foreignKeyColumn, idValue);
        return targetProxy.query().where(condition).one().orElse(null);
    }

    /**
     * 加载 OneToMany 关联
     */
    List<?> fetchOneToMany(Object idValue, RelationMetadata relation, Class<?> targetEntityClass) {
        String foreignKeyColumn = relation.isOwningSide()
                ? relation.getForeignKeyColumn()
                : relation.getMappedBy() + "_id";

        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        Condition condition = Conditions.eq(foreignKeyColumn, idValue);
        return targetProxy.query().where(condition).list();
    }

    /**
     * 加载 ManyToMany 关联
     */
    Set<?> fetchManyToMany(Object idValue, RelationMetadata relation, Class<?> targetEntityClass) {
        String joinTable = relation.getJoinTable();
        String joinColumn = relation.getJoinColumn();
        String inverseJoinColumn = relation.getInverseJoinColumn();
        String targetTableName = inferTableName(targetEntityClass);

        String sql;
        if (relation.isOwningSide()) {
            sql = buildManyToManySql(targetTableName, joinTable, inverseJoinColumn, joinColumn);
        } else {
            sql = buildManyToManySql(targetTableName, joinTable, joinColumn, inverseJoinColumn);
        }

        return executeManyToManyQuery(sql, idValue, targetEntityClass);
    }

    // ==================== 批量加载 ====================

    /**
     * 批量加载 ManyToOne 关联
     */
    <T> void fetchAllManyToOne(Iterable<T> entities, RelationMetadata relation,
                               Class<?> targetEntityClass, EntityMetadata<?> metadata) {
        // 收集所有外键值
        Set<Object> foreignKeyValues = new LinkedHashSet<>();
        String foreignKeyFieldName = findForeignKeyFieldName(relation, metadata);

        for (T entity : entities) {
            Object foreignKeyValue = getFieldValueByReflection(entity, foreignKeyFieldName);
            if (foreignKeyValue != null) {
                foreignKeyValues.add(foreignKeyValue);
            }
        }

        if (foreignKeyValues.isEmpty()) {
            return;
        }

        // 批量查询目标实体
        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        Map<Object, Object> targetEntityMap = new HashMap<>();
        for (Object target : targetProxy.findAllById(foreignKeyValues)) {
            Object targetId = getIdValueFromEntity(target, targetEntityClass);
            if (targetId != null) {
                targetEntityMap.put(targetId, target);
            }
        }

        // 将关联实体设置到源实体
        String fieldName = relation.getFieldName();
        for (T entity : entities) {
            Object foreignKeyValue = getFieldValueByReflection(entity, foreignKeyFieldName);
            if (foreignKeyValue != null) {
                Object target = targetEntityMap.get(foreignKeyValue);
                setFieldValue(entity, fieldName, target);
            }
        }
    }

    /**
     * 批量加载 OneToOne 关联
     */
    <T> void fetchAllOneToOne(Iterable<T> entities, List<Object> ids, RelationMetadata relation,
                              Class<?> targetEntityClass, EntityMetadata<?> metadata) {
        if (relation.isOwningSide()) {
            fetchAllManyToOne(entities, relation, targetEntityClass, metadata);
            return;
        }

        // 目标实体持有外键，批量反向查询
        String mappedBy = relation.getMappedBy();
        String foreignKeyColumn = mappedBy + "_id";

        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        Condition condition = Conditions.in(foreignKeyColumn, ids);
        List<?> targets = targetProxy.query().where(condition).list();

        // 构建外键到目标实体的映射
        Map<Object, Object> foreignKeyToTarget = new HashMap<>();
        for (Object target : targets) {
            Object foreignKeyValue = getFieldValueFromEntity(target, foreignKeyColumn, targetEntityClass);
            if (foreignKeyValue != null) {
                foreignKeyToTarget.put(foreignKeyValue, target);
            }
        }

        // 设置到源实体
        String fieldName = relation.getFieldName();
        for (T entity : entities) {
            Object id = getIdValueFromEntity(entity, entity.getClass());
            if (id != null) {
                Object target = foreignKeyToTarget.get(id);
                setFieldValue(entity, fieldName, target);
            }
        }
    }

    /**
     * 批量加载 OneToMany 关联
     * <p>
     * 返回外键到目标实体列表的映射，由调用方根据需要处理
     */
    Map<Object, List<Object>> fetchAllOneToMany(List<Object> ids, RelationMetadata relation,
                                                Class<?> targetEntityClass) {
        String foreignKeyColumn = relation.isOwningSide()
                ? relation.getForeignKeyColumn()
                : relation.getMappedBy() + "_id";

        EntityProxy<?> targetProxy = dataManager.entity(targetEntityClass);
        Condition condition = Conditions.in(foreignKeyColumn, ids);
        List<?> targets = targetProxy.query().where(condition).list();

        // 构建外键到目标实体列表的映射
        Map<Object, List<Object>> foreignKeyToTargets = new HashMap<>();
        for (Object target : targets) {
            Object foreignKeyValue = getFieldValueFromEntity(target, foreignKeyColumn, targetEntityClass);
            if (foreignKeyValue != null) {
                foreignKeyToTargets.computeIfAbsent(foreignKeyValue, k -> new ArrayList<>()).add(target);
            }
        }

        return foreignKeyToTargets;
    }

    /**
     * 批量加载 ManyToMany 关联
     * <p>
     * 返回源ID到目标实体集合的映射
     */
    Map<Object, Set<Object>> fetchAllManyToMany(List<Object> ids, RelationMetadata relation,
                                                Class<?> targetEntityClass) {
        String joinTable = relation.getJoinTable();
        String joinColumn = relation.getJoinColumn();
        String inverseJoinColumn = relation.getInverseJoinColumn();
        String targetTableName = inferTableName(targetEntityClass);

        // 构建批量查询 SQL
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        String sql;
        if (relation.isOwningSide()) {
            sql = "SELECT j." + dialect.quoteIdentifier(joinColumn) + " as source_id, t.* FROM " +
                    dialect.quoteIdentifier(targetTableName) + " t " +
                    "INNER JOIN " + dialect.quoteIdentifier(joinTable) + " j " +
                    "ON t.id = j." + dialect.quoteIdentifier(inverseJoinColumn) + " " +
                    "WHERE j." + dialect.quoteIdentifier(joinColumn) + " IN (" + placeholders + ")";
        } else {
            sql = "SELECT j." + dialect.quoteIdentifier(inverseJoinColumn) + " as source_id, t.* FROM " +
                    dialect.quoteIdentifier(targetTableName) + " t " +
                    "INNER JOIN " + dialect.quoteIdentifier(joinTable) + " j " +
                    "ON t.id = j." + dialect.quoteIdentifier(joinColumn) + " " +
                    "WHERE j." + dialect.quoteIdentifier(inverseJoinColumn) + " IN (" + placeholders + ")";
        }

        // 执行查询并构建映射
        return executeManyToManyBatchQuery(sql, ids, targetEntityClass);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建 ManyToMany 查询 SQL
     */
    private String buildManyToManySql(String targetTableName, String joinTable,
                                       String joinColumnOnTarget, String joinColumnOnSource) {
        return "SELECT t.* FROM " + dialect.quoteIdentifier(targetTableName) +
                " t INNER JOIN " + dialect.quoteIdentifier(joinTable) +
                " j ON t.id = j." + dialect.quoteIdentifier(joinColumnOnTarget) +
                " WHERE j." + dialect.quoteIdentifier(joinColumnOnSource) + " = ?";
    }

    /**
     * 执行 ManyToMany 查询
     */
    @SuppressWarnings("unchecked")
    private Set<?> executeManyToManyQuery(String sql, Object idValue, Class<?> targetEntityClass) {
        JdbcEntityProxy<?> targetProxy = (JdbcEntityProxy<?>) dataManager.entity(targetEntityClass);
        List<?> results = targetProxy.dataManager.queryForList(sql, List.of(idValue), targetProxy.rowMapper);
        return new LinkedHashSet<>(results);
    }

    /**
     * 执行 ManyToMany 批量查询
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Set<Object>> executeManyToManyBatchQuery(String sql, List<Object> ids,
                                                                  Class<?> targetEntityClass) {
        JdbcEntityProxy<?> targetProxy = (JdbcEntityProxy<?>) dataManager.entity(targetEntityClass);

        Map<Object, Set<Object>> result = new HashMap<>();
        List<?> rows = targetProxy.dataManager.queryForList(sql, ids, (io.github.afgprojects.framework.data.core.mapper.ResultMapper<Object[]>) (rs, rowNum) -> {
            // 提取 source_id 和目标实体
            Object sourceId = rs.getObject("source_id");
            return new Object[]{sourceId, targetProxy.rowMapper.mapRow(rs, rowNum)};
        });

        for (Object row : rows) {
            Object[] pair = (Object[]) row;
            result.computeIfAbsent(pair[0], k -> new LinkedHashSet<>()).add(pair[1]);
        }

        return result;
    }

    /**
     * 获取外键值
     */
    private @Nullable Object getForeignKeyValue(Object entity, RelationMetadata relation,
                                                EntityMetadata<?> metadata) {
        String foreignKeyFieldName = findForeignKeyFieldName(relation, metadata);
        return getFieldValueByReflection(entity, foreignKeyFieldName);
    }

    /**
     * 查找外键字段名
     */
    String findForeignKeyFieldName(RelationMetadata relation, EntityMetadata<?> metadata) {
        String foreignKeyColumn = relation.getForeignKeyColumn();
        String foreignKeyField = columnNameToFieldName(foreignKeyColumn);

        FieldMetadata field = metadata.getField(foreignKeyField);
        if (field == null) {
            foreignKeyField = relation.getFieldName() + "Id";
            field = metadata.getField(foreignKeyField);
        }

        if (field == null) {
            throw new IllegalStateException(
                    "Foreign key field not found for association '" + relation.getFieldName() +
                    "' in entity " + metadata.getEntityClass().getSimpleName());
        }

        return field.getPropertyName();
    }

    /**
     * 通过反射获取字段值
     */
    private @Nullable Object getFieldValueByReflection(Object entity, String fieldName) {
        try {
            Field field = findDeclaredFieldCached(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(entity);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }

    /**
     * 在类层次结构中查找字段（带缓存）
     */
    private Field findDeclaredFieldCached(Class<?> clazz, String fieldName) {
        return fieldCache
                .computeIfAbsent(clazz, k -> new HashMap<>())
                .computeIfAbsent(fieldName, fn -> findDeclaredFieldUncached(clazz, fn));
    }

    /**
     * 在类层次结构中查找字段（无缓存）
     */
    private Field findDeclaredFieldUncached(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }

    /**
     * 列名转字段名（snake_case to camelCase）
     *
     * @see NamingUtils#columnNameToFieldName(String)
     */
    String columnNameToFieldName(String columnName) {
        return NamingUtils.columnNameToFieldName(columnName);
    }

    /**
     * 根据实体类推断表名
     *
     * @see NamingUtils#inferTableName(Class)
     */
    String inferTableName(Class<?> entityClass) {
        return NamingUtils.inferTableName(entityClass);
    }

    /**
     * 从实体中获取 ID 值
     */
    private Object getIdValueFromEntity(Object entity, Class<?> entityClass) {
        try {
            Field idField = findDeclaredFieldCached(entityClass, "id");
            if (idField != null) {
                idField.setAccessible(true);
                return idField.get(entity);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从实体中获取指定字段的值
     */
    private Object getFieldValueFromEntity(Object entity, String columnName, Class<?> entityClass) {
        String fieldName = columnNameToFieldName(columnName);
        try {
            Field field = getDeclaredField(entityClass, fieldName);
            field.setAccessible(true);
            return getFieldValue(field, entity);
        } catch (NoSuchFieldException e) {
            // 尝试添加 "_id" 后缀
            try {
                Field field = getDeclaredField(entityClass, fieldName + "Id");
                field.setAccessible(true);
                return getFieldValue(field, entity);
            } catch (Exception ex) {
                return null;
            }
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /**
     * 获取类的声明字段，使用缓存（可被子类覆盖用于测试）
     *
     * @param entityClass 实体类
     * @param fieldName   字段名
     * @return 字段对象
     * @throws NoSuchFieldException 如果字段不存在
     */
    Field getDeclaredField(Class<?> entityClass, String fieldName) throws NoSuchFieldException {
        Field field = findDeclaredFieldCached(entityClass, fieldName);
        if (field == null) {
            throw new NoSuchFieldException(fieldName);
        }
        return field;
    }

    /**
     * 获取字段的值，可被子类覆盖用于测试
     *
     * @param field  字段对象
     * @param entity 实体对象
     * @return 字段值
     * @throws IllegalAccessException 如果无法访问字段
     */
    Object getFieldValue(Field field, Object entity) throws IllegalAccessException {
        return field.get(entity);
    }

    /**
     * 设置实体字段值（使用字段缓存）
     */
    void setFieldValue(Object entity, String fieldName, Object value) {
        try {
            Field field = findDeclaredFieldCached(entity.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(entity, value);
            } else {
                throw new RuntimeException(
                        "Field '" + fieldName + "' not found on entity " + entity.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to set association field '" + fieldName + "' on entity " + entity.getClass().getSimpleName(),
                    e
            );
        }
    }
}
