package io.github.afgprojects.framework.data.core.metadata;

/**
 * 字段元数据
 */
public interface FieldMetadata {

    /**
     * 属性名
     */
    String getPropertyName();

    /**
     * 列名
     */
    String getColumnName();

    /**
     * 字段类型
     */
    Class<?> getFieldType();

    /**
     * 是否主键
     */
    boolean isId();

    /**
     * 是否自动生成
     */
    boolean isGenerated();
}
