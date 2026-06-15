package io.github.afgprojects.framework.data.jdbc;

import io.github.afgprojects.framework.data.core.dialect.Dialect;
import io.github.afgprojects.framework.data.core.metadata.EntityMetadata;
import io.github.afgprojects.framework.data.core.metadata.FieldMetadata;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * SQL 构建器
 * <p>
 * 负责构建 INSERT、UPDATE、SELECT、DELETE 等 SQL 语句，支持缓存优化。
 * 从 EntityQueryHelper 中提取，专注于 SQL 生成逻辑。
 *
 * @param <T> 实体类型
 */
class SqlBuilder<T> {

    private final Class<T> entityClass;
    private final Dialect dialect;
    private final EntityMetadata<T> metadata;

    /**
     * SQL 缓存（延迟初始化）
     */
    private volatile String insertSql;
    private volatile String insertWithIdSql;
    private volatile String updateSql;
    private volatile String updateVersionedSql;
    private volatile String selectBaseSql;

    SqlBuilder(Class<T> entityClass, Dialect dialect, EntityMetadata<T> metadata) {
        this.entityClass = entityClass;
        this.dialect = dialect;
        this.metadata = metadata;
    }

    /**
     * 构建 INSERT SQL（带缓存）
     */
    String buildInsertSql() {
        if (insertSql == null) {
            synchronized (this) {
                if (insertSql == null) {
                    insertSql = doBuildInsertSql();
                }
            }
        }
        return insertSql;
    }

    private String doBuildInsertSql() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isGenerated()) {
                columns.add(dialect.quoteIdentifier(field.getColumnName()));
                placeholders.add("?");
            }
        }
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (").append(String.join(", ", placeholders)).append(")");
        return sql.toString();
    }

    /**
     * 构建包含 ID 的 INSERT SQL（用于预设 ID 的插入，带缓存）
     */
    String buildInsertWithIdSql() {
        if (insertWithIdSql == null) {
            synchronized (this) {
                if (insertWithIdSql == null) {
                    insertWithIdSql = doBuildInsertWithIdSql();
                }
            }
        }
        return insertWithIdSql;
    }

    private String doBuildInsertWithIdSql() {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        for (var field : metadata.getFields()) {
            columns.add(dialect.quoteIdentifier(field.getColumnName()));
            placeholders.add("?");
        }
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (").append(String.join(", ", placeholders)).append(")");
        return sql.toString();
    }

    /**
     * 构建 UPDATE SQL（带缓存）
     *
     * @param isVersioned 是否为版本化实体
     * @return UPDATE SQL
     */
    String buildUpdateSql(boolean isVersioned) {
        if (isVersioned) {
            if (updateVersionedSql == null) {
                synchronized (this) {
                    if (updateVersionedSql == null) {
                        updateVersionedSql = doBuildUpdateSql(true);
                    }
                }
            }
            return updateVersionedSql;
        } else {
            if (updateSql == null) {
                synchronized (this) {
                    if (updateSql == null) {
                        updateSql = doBuildUpdateSql(false);
                    }
                }
            }
            return updateSql;
        }
    }

    private String doBuildUpdateSql(boolean isVersioned) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName())).append(" SET ");

        List<String> setParts = new ArrayList<>();
        for (var field : metadata.getFields()) {
            if (!field.isId() && !field.isGenerated()) {
                String quotedColumn = dialect.quoteIdentifier(field.getColumnName());
                if (isVersioned && "version".equals(field.getPropertyName())) {
                    setParts.add(quotedColumn + " = " + quotedColumn + " + 1");
                } else {
                    setParts.add(quotedColumn + " = ?");
                }
            }
        }
        sql.append(String.join(", ", setParts));
        sql.append(" WHERE ").append(dialect.quoteIdentifier(metadata.getIdField().getColumnName())).append(" = ?");

        if (isVersioned) {
            // 从元数据获取 version 字段列名
            String versionColumnName = metadata.getField("version") != null
                    ? metadata.getField("version").getColumnName() : "version";
            sql.append(" AND ").append(dialect.quoteIdentifier(versionColumnName)).append(" = ?");
        }

        return sql.toString();
    }

    /**
     * 获取基础 SELECT SQL（不带 WHERE，带缓存）
     */
    String getSelectBaseSql() {
        if (selectBaseSql == null) {
            synchronized (this) {
                if (selectBaseSql == null) {
                    StringBuilder sql = new StringBuilder("SELECT ");
                    List<String> columns = new ArrayList<>();
                    for (var field : metadata.getFields()) {
                        columns.add(dialect.quoteIdentifier(field.getColumnName()));
                    }
                    sql.append(String.join(", ", columns));
                    sql.append(" FROM ").append(dialect.quoteIdentifier(metadata.getTableName()));
                    selectBaseSql = sql.toString();
                }
            }
        }
        return selectBaseSql;
    }

    /**
     * 构建 SELECT SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(@Nullable String whereClause) {
        StringBuilder sql = new StringBuilder(getSelectBaseSql());
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    /**
     * 构建 SELECT SQL（指定字段）
     *
     * @param fields      要查询的字段列表
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return SELECT SQL
     */
    String buildSelectSql(List<String> fields, @Nullable String whereClause) {
        StringBuilder sql = new StringBuilder("SELECT ");
        if (fields == null || fields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", fields));
        }
        sql.append(" FROM ").append(dialect.quoteIdentifier(metadata.getTableName()));
        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    /**
     * 构建 DELETE SQL
     *
     * @param whereClause WHERE 子句（不含 WHERE 关键字）
     * @return DELETE SQL
     */
    String buildDeleteSql(@Nullable String whereClause) {
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(dialect.quoteIdentifier(metadata.getTableName()));

        if (whereClause != null && !whereClause.isEmpty()) {
            sql.append(" WHERE ").append(whereClause);
        }
        return sql.toString();
    }

    /**
     * 获取表名
     */
    String getTableName() {
        return metadata.getTableName();
    }

    /**
     * 获取实体类
     */
    Class<T> getEntityClass() {
        return entityClass;
    }

    /**
     * 获取元数据
     */
    EntityMetadata<T> getMetadata() {
        return metadata;
    }

    /**
     * 获取方言
     */
    Dialect getDialect() {
        return dialect;
    }
}
