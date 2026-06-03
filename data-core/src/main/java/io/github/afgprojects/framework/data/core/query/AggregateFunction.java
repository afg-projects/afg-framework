package io.github.afgprojects.framework.data.core.query;

/**
 * 聚合函数枚举
 *
 * @author afg
 */
public enum AggregateFunction {

    COUNT,
    COUNT_DISTINCT,
    SUM,
    AVG,
    MAX,
    MIN;

    /**
     * 获取 SQL 关键字
     */
    public String getSqlKeyword() {
        return switch (this) {
            case COUNT, COUNT_DISTINCT -> "COUNT";
            case SUM -> "SUM";
            case AVG -> "AVG";
            case MAX -> "MAX";
            case MIN -> "MIN";
        };
    }
}
