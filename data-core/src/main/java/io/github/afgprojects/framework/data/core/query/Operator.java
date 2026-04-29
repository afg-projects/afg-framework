package io.github.afgprojects.framework.data.core.query;

/**
 * 操作符枚举
 */
public enum Operator {

    /**
     * 等于 (=)
     */
    EQ("="),

    /**
     * 不等于 (!=)
     */
    NE("!="),

    /**
     * 大于 (>)
     */
    GT(">"),

    /**
     * 大于等于 (>=)
     */
    GE(">="),

    /**
     * 小于 (<)
     */
    LT("<"),

    /**
     * 小于等于 (<=)
     */
    LE("<="),

    /**
     * LIKE（包含）
     */
    LIKE("LIKE"),

    /**
     * 左模糊匹配（以...开头）
     */
    LIKE_LEFT("LIKE"),

    /**
     * 右模糊匹配（以...结尾）
     */
    LIKE_RIGHT("LIKE"),

    /**
     * NOT LIKE
     */
    NOT_LIKE("NOT LIKE"),

    /**
     * IN
     */
    IN("IN"),

    /**
     * NOT IN
     */
    NOT_IN("NOT IN"),

    /**
     * IS NULL
     */
    IS_NULL("IS NULL"),

    /**
     * IS NOT NULL
     */
    IS_NOT_NULL("IS NOT NULL"),

    /**
     * BETWEEN
     */
    BETWEEN("BETWEEN"),

    /**
     * NOT BETWEEN
     */
    NOT_BETWEEN("NOT BETWEEN");

    private final String symbol;

    Operator(String symbol) {
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

    /**
     * 是否需要值
     *
     * @return true表示需要值
     */
    public boolean requiresValue() {
        return this != IS_NULL && this != IS_NOT_NULL;
    }
}