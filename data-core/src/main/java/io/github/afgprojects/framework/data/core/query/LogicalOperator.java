package io.github.afgprojects.framework.data.core.query;

/**
 * 逻辑操作符枚举
 */
public enum LogicalOperator {

    /**
     * AND 连接
     */
    AND("AND"),

    /**
     * OR 连接
     */
    OR("OR");

    private final String symbol;

    LogicalOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * 获取操作符符号
     *
     * @return 符号
     */
    public String getSymbol() {
        return symbol;
    }
}