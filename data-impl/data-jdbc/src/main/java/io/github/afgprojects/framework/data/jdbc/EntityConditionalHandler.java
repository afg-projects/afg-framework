package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.query.Condition;
import io.github.afgprojects.framework.data.core.safety.FullTableOperationChecker;
import io.github.afgprojects.framework.data.jdbc.cache.EntityCacheHandler;
import io.github.afgprojects.framework.data.sql.converter.ConditionToSqlConverter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 实体条件操作处理器
 * <p>
 * 封装条件更新和条件删除相关的逻辑。
 * 从 JdbcEntityProxy 中提取，降低类的复杂度。
 *
 * @param <T> 实体类型
 */
@Slf4j
public class EntityConditionalHandler<T> {

    private final Class<T> entityClass;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;
    private final JdbcDataManager dataManager;
    private final EntityCacheHandler<T> cacheHandler;

    /**
     * 全表操作检查器（可注入）
     */
    private FullTableOperationChecker fullTableOperationChecker;

    public EntityConditionalHandler(Class<T> entityClass, Dialect dialect,
                                    EntityMetadata<T> metadata, JdbcDataManager dataManager,
                                    EntityCacheHandler<T> cacheHandler) {
        this.entityClass = entityClass;
        this.dialect = dialect;
        this.metadata = metadata;
        this.dataManager = dataManager;
        this.cacheHandler = cacheHandler;
        this.fullTableOperationChecker = dataManager.getFullTableOperationChecker();
    }

    /**
     * 设置全表操作检查器
     *
     * @param checker 全表操作检查器
     */
    public void setFullTableOperationChecker(@NonNull FullTableOperationChecker checker) {
        this.fullTableOperationChecker = checker;
    }

    /**
     * 根据条件批量更新实体
     *
     * @param condition 更新条件
     * @param updates   更新内容
     * @return 受影响的行数
     */
    public long updateAll(@NonNull Condition condition, @NonNull Map<String, Object> updates) {
        // 全表操作保护：检查条件是否为空
        fullTableOperationChecker.check("updateAll", entityClass, condition);

        long affected = executeConditionalUpdate(condition, updates);
        if (affected > 0) {
            cacheHandler.clear();
        }
        return affected;
    }

    /**
     * 根据条件批量删除实体
     *
     * @param condition 删除条件
     * @return 受影响的行数
     */
    public long deleteByCondition(@NonNull Condition condition) {
        // 全表操作保护：检查条件是否为空
        fullTableOperationChecker.check("deleteByCondition", entityClass, condition);

        long affected = executeConditionalDelete(condition);
        if (affected > 0) {
            cacheHandler.clear();
        }
        return affected;
    }

    /**
     * 验证字段名是否在元数据中存在
     *
     * @param fieldName 字段名
     * @throws BusinessException 如果字段名无效
     */
    private void validateFieldName(String fieldName) {
        if (metadata.getField(fieldName) == null) {
            throw new BusinessException(CommonErrorCode.FIELD_NOT_FOUND,
                String.format("Invalid field name '%s' for entity '%s'. Field does not exist in entity metadata.",
                    fieldName, entityClass.getSimpleName()));
        }
    }

    /**
     * 执行条件更新
     */
    private long executeConditionalUpdate(@NonNull Condition condition, @NonNull Map<String, Object> updates) {
        // 验证所有字段名是否在元数据中存在
        for (String fieldName : updates.keySet()) {
            validateFieldName(fieldName);
        }

        ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
        ConditionToSqlConverter.SqlResult whereResult = converter.convert(condition);

        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" SET ");

        List<String> setParts = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String fieldName = entry.getKey();
            // 将字段名转换为数据库列名
            var fieldMetadata = metadata.getField(fieldName);
            String columnName = fieldMetadata != null ? fieldMetadata.getColumnName() : fieldName;
            // 使用 dialect.quoteIdentifier() 对列名进行引用，防止 SQL 注入
            setParts.add(dialect.quoteIdentifier(columnName) + " = ?");
            params.add(JdbcTypeConverter.convertForJdbc(entry.getValue()));
        }
        sql.append(String.join(", ", setParts));
        sql.append(" WHERE ").append(whereResult.sql());
        params.addAll(JdbcTypeConverter.convertParamsForJdbc(whereResult.parameters()));

        return dataManager.executeUpdate(sql.toString(), params);
    }

    /**
     * 执行条件删除
     */
    private long executeConditionalDelete(@NonNull Condition condition) {
        ConditionToSqlConverter converter = new ConditionToSqlConverter(dialect);
        ConditionToSqlConverter.SqlResult result = converter.convert(condition);
        String sql = "DELETE FROM " + dialect.quoteIdentifier(metadata.getTableName()) + " WHERE " + result.sql();
        return dataManager.executeUpdate(sql, JdbcTypeConverter.convertParamsForJdbc(result.parameters()));
    }
}
