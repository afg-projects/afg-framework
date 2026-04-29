package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Schema 元数据
 * <p>
 * 统一的数据库表结构元数据抽象，用于实体、数据库、Liquibase ChangeLog 之间的转换
 */
public interface SchemaMetadata {

    /**
     * 表名
     */
    String getTableName();

    /**
     * 表注释
     */
    @Nullable String getComment();

    /**
     * 所有列
     */
    List<ColumnMetadata> getColumns();

    /**
     * 主键
     */
    @Nullable PrimaryKeyMetadata getPrimaryKey();

    /**
     * 所有索引
     */
    List<IndexMetadata> getIndexes();

    /**
     * 所有外键
     */
    List<ForeignKeyMetadata> getForeignKeys();

    /**
     * 根据列名获取列元数据
     */
    default @Nullable ColumnMetadata getColumn(String columnName) {
        for (ColumnMetadata column : getColumns()) {
            if (column.getColumnName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    /**
     * 是否有指定列
     */
    default boolean hasColumn(String columnName) {
        return getColumn(columnName) != null;
    }
}
