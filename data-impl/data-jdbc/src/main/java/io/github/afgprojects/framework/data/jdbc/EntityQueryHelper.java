package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.mapper.TypeHandlerRegistry;
import io.github.afgprojects.framework.data.jdbc.util.NamingUtils;
import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.util.List;

/**
 * 实体查询辅助类（门面模式）
 * <p>
 * 作为门面类，组合 {@link SqlBuilder}、{@link ParameterExtractor}、{@link EntityMapper} 三个组件，
 * 提供统一的 SQL 构建、参数提取、结果映射接口，保持向后兼容。
 * <p>
 * 职责分离：
 * <ul>
 *   <li>{@link SqlBuilder} - SQL 构建（INSERT、UPDATE、SELECT、DELETE）</li>
 *   <li>{@link ParameterExtractor} - 参数提取（INSERT、UPDATE 参数，ID 值访问）</li>
 *   <li>{@link EntityMapper} - 结果映射（ResultSet 到实体，类型转换，命名转换）</li>
 * </ul>
 *
 * @param <T> 实体类型
 */
class EntityQueryHelper<T> {

    private final SqlBuilder<T> sqlBuilder;
    private final ParameterExtractor<T> parameterExtractor;
    private final EntityMapper<T> entityMapper;

    EntityQueryHelper(Class<T> entityClass, Dialect dialect, EntityMetadata<T> metadata) {
        this(entityClass, dialect, metadata, TypeHandlerRegistry.defaultRegistry());
    }

    EntityQueryHelper(Class<T> entityClass, Dialect dialect, EntityMetadata<T> metadata, TypeHandlerRegistry typeHandlerRegistry) {
        this.sqlBuilder = new SqlBuilder<>(entityClass, dialect, metadata);
        this.parameterExtractor = new ParameterExtractor<>(entityClass, metadata);
        this.entityMapper = new EntityMapper<>(entityClass, metadata, typeHandlerRegistry);
    }

    // ==================== SQL 构建（委托给 SqlBuilder） ====================

    /**
     * 构建 INSERT SQL（带缓存）
     */
    String buildInsertSql() {
        return sqlBuilder.buildInsertSql();
    }

    /**
     * 构建包含 ID 的 INSERT SQL（用于预设 ID 的插入，带缓存）
     */
    String buildInsertWithIdSql() {
        return sqlBuilder.buildInsertWithIdSql();
    }

    /**
     * 构建 UPDATE SQL（带缓存）
     *
     * @param isVersioned 是否为版本化实体
     * @return UPDATE SQL
     */
    String buildUpdateSql(boolean isVersioned) {
        return sqlBuilder.buildUpdateSql(isVersioned);
    }

    /**
     * 获取基础 SELECT SQL（不带 WHERE，带缓存）
     */
    String getSelectBaseSql() {
        return sqlBuilder.getSelectBaseSql();
    }

    /**
     * 构建 SELECT SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(@Nullable String whereClause) {
        return sqlBuilder.buildSelectSql(whereClause);
    }

    /**
     * 构建 SELECT SQL（指定字段）
     *
     * @param fields      要查询的字段列表
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(List<String> fields, @Nullable String whereClause) {
        return sqlBuilder.buildSelectSql(fields, whereClause);
    }

    /**
     * 构建 DELETE SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return DELETE SQL
     */
    String buildDeleteSql(@Nullable String whereClause) {
        return sqlBuilder.buildDeleteSql(whereClause);
    }

    // ==================== 参数提取（委托给 ParameterExtractor） ====================

    /**
     * 提取 INSERT 参数
     */
    List<Object> extractInsertParams(T entity) {
        return parameterExtractor.extractInsertParams(entity);
    }

    /**
     * 提取包含 ID 的 INSERT 参数（用于预设 ID 的插入）
     */
    List<Object> extractInsertWithIdParams(T entity) {
        return parameterExtractor.extractInsertWithIdParams(entity);
    }

    /**
     * 提取 UPDATE 参数
     *
     * @param entity      实体对象
     * @param isVersioned 是否为版本化实体
     * @return 参数列表
     */
    List<Object> extractUpdateParams(T entity, boolean isVersioned) {
        return parameterExtractor.extractUpdateParams(entity, isVersioned);
    }

    /**
     * 获取 ID 值
     */
    @Nullable Object getIdValue(T entity) {
        return parameterExtractor.getIdValue(entity);
    }

    /**
     * 设置 ID 值
     */
    void setIdValue(T entity, long id) {
        parameterExtractor.setIdValue(entity, id);
    }

    // ==================== 结果映射（委托给 EntityMapper） ====================

    /**
     * 映射结果集行到实体
     */
    T mapRow(ResultSet rs, int rowNum) {
        return entityMapper.mapRow(rs, rowNum);
    }

    // ==================== 工具方法（委托给 NamingUtils） ====================

    /**
     * 列名转字段名（snake_case to camelCase）
     * <p>
     * 特殊处理阿里规约 boolean 字段：
     * <ul>
     *   <li>列名 is_active → 字段名 active（如果实体有 active 字段）</li>
     *   <li>列名 is_deleted → 字段名 deleted（如果实体有 deleted 字段）</li>
     * </ul>
     *
     * @see NamingUtils#columnNameToFieldName(String, EntityMetadata)
     */
    String columnNameToFieldName(String columnName) {
        return NamingUtils.columnNameToFieldName(columnName, sqlBuilder.getMetadata());
    }

    /**
     * 字段名转列名（camelCase to snake_case）
     *
     * @see NamingUtils#fieldNameToColumnName(String)
     */
    String fieldNameToColumnName(String fieldName) {
        return NamingUtils.fieldNameToColumnName(fieldName);
    }

    // ==================== 访问器 ====================

    /**
     * 获取表名
     */
    String getTableName() {
        return sqlBuilder.getTableName();
    }

    /**
     * 获取实体类
     */
    Class<T> getEntityClass() {
        return sqlBuilder.getEntityClass();
    }

    /**
     * 获取元数据
     */
    EntityMetadata<T> getMetadata() {
        return sqlBuilder.getMetadata();
    }

    /**
     * 获取方言
     */
    Dialect getDialect() {
        return sqlBuilder.getDialect();
    }

    /**
     * 获取 SQL 构建器
     * <p>
     * 用于需要直接访问 SqlBuilder 的场景
     */
    SqlBuilder<T> getSqlBuilder() {
        return sqlBuilder;
    }

    /**
     * 获取参数提取器
     * <p>
     * 用于需要直接访问 ParameterExtractor 的场景
     */
    ParameterExtractor<T> getParameterExtractor() {
        return parameterExtractor;
    }

    /**
     * 获取实体映射器
     * <p>
     * 用于需要直接访问 EntityMapper 的场景
     */
    EntityMapper<T> getEntityMapper() {
        return entityMapper;
    }
}
