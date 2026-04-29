package io.github.afgprojects.framework.data.core.schema;

/**
 * 索引元数据
 */
public interface IndexMetadata {

    /**
     * 索引名
     */
    String getIndexName();

    /**
     * 索引列名列表
     */
    java.util.List<String> getColumnNames();

    /**
     * 是否唯一索引
     */
    boolean isUnique();

    /**
     * 索引类型（如 BTREE、HASH）
     */
    String getIndexType();
}
