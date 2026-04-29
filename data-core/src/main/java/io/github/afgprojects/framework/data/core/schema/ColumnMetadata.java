package io.github.afgprojects.framework.data.core.schema;

import org.jspecify.annotations.Nullable;

/**
 * 列元数据
 */
public interface ColumnMetadata {

    /**
     * 列名
     */
    String getColumnName();

    /**
     * 数据类型（SQL 类型，如 VARCHAR(255)、BIGINT）
     */
    String getDataType();

    /**
     * 是否可空
     */
    boolean isNullable();

    /**
     * 默认值
     */
    @Nullable String getDefaultValue();

    /**
     * 注释
     */
    @Nullable String getComment();

    /**
     * 是否唯一
     */
    boolean isUnique();

    /**
     * 是否主键
     */
    boolean isPrimaryKey();

    /**
     * 是否自动生成
     */
    boolean isAutoIncrement();
}
