package io.github.afgprojects.framework.data.core.schema;

/**
 * 主键元数据
 */
public interface PrimaryKeyMetadata {

    /**
     * 主键约束名
     */
    String getConstraintName();

    /**
     * 主键列名列表（支持复合主键）
     */
    java.util.List<String> getColumnNames();
}
