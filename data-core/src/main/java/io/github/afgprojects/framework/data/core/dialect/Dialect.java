package io.github.afgprojects.framework.data.core.dialect;

import io.github.afgprojects.framework.data.core.page.PageRequest;
import org.jspecify.annotations.NonNull;

/**
 * 数据库方言接口
 * <p>
 * 定义不同数据库的 SQL 语法差异处理
 */
public interface Dialect {

    /**
     * 获取数据库类型
     */
    @NonNull DatabaseType getDatabaseType();

    // ==================== 分页 ====================

    /**
     * 生成分页 SQL
     *
     * @param sql    原始 SQL
     * @param offset 偏移量
     * @param limit  限制数量
     * @return 分页 SQL
     */
    @NonNull String getPaginationSql(@NonNull String sql, long offset, long limit);

    /**
     * 生成分页 SQL
     *
     * @param sql       原始 SQL
     * @param pageable  分页参数
     * @return 分页 SQL
     */
    @NonNull String getPaginationSql(@NonNull String sql, @NonNull PageRequest pageable);

    /**
     * 是否支持 LIMIT OFFSET 语法
     */
    boolean supportsLimitOffset();

    /**
     * 是否支持 FETCH FIRST 语法（SQL 标准）
     */
    boolean supportsFetchFirst();

    // ==================== 标识符 ====================

    /**
     * 获取标识符引用字符（如 MySQL 用反引号，Oracle 用双引号）
     */
    @NonNull String getIdentifierQuote();

    /**
     * 引用标识符
     */
    @NonNull String quoteIdentifier(@NonNull String identifier);

    // ==================== 函数 ====================

    /**
     * 获取当前时间函数
     */
    @NonNull String getCurrentTimeFunction();

    /**
     * 获取当前日期函数
     */
    @NonNull String getCurrentDateFunction();

    /**
     * 获取当前时间戳函数
     */
    @NonNull String getCurrentTimestampFunction();

    // ==================== 主键生成 ====================

    /**
     * 是否支持自增主键
     */
    boolean supportsAutoIncrement();

    /**
     * 是否支持序列
     */
    boolean supportsSequence();

    /**
     * 获取自增主键语法
     */
    @NonNull String getAutoIncrementSyntax();

    /**
     * 获取序列查询 SQL
     *
     * @param sequenceName 序列名
     * @return 查询下一个值的 SQL
     */
    @NonNull String getSequenceNextValueSql(@NonNull String sequenceName);

    // ==================== 类型映射 ====================

    /**
     * 获取 Java 类型对应的数据库类型
     */
    @NonNull String getSqlType(@NonNull Class<?> javaType);

    // ==================== 其他 ====================

    /**
     * 获取 LIKE 表达式的通配符
     */
    @NonNull String getLikeWildcard();

    /**
     * 是否支持 FOR UPDATE
     */
    boolean supportsForUpdate();

    /**
     * 获取 FOR UPDATE 语法
     */
    @NonNull String getForUpdateSyntax();
}