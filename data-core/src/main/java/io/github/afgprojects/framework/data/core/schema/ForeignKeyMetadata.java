package io.github.afgprojects.framework.data.core.schema;

/**
 * 外键元数据
 */
public interface ForeignKeyMetadata {

    /**
     * 外键约束名
     */
    String getConstraintName();

    /**
     * 外键列名列表（当前表）
     */
    java.util.List<String> getColumnNames();

    /**
     * 引用表名
     */
    String getReferencedTableName();

    /**
     * 引用列名列表（引用表）
     */
    java.util.List<String> getReferencedColumnNames();

    /**
     * 更新规则（如 CASCADE、RESTRICT、NO ACTION）
     */
    String getUpdateRule();

    /**
     * 删除规则
     */
    String getDeleteRule();
}
