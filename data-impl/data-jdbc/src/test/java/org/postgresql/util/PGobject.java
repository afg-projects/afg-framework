package org.postgresql.util;

/**
 * PGobject 测试替身
 * <p>
 * 模拟 PostgreSQL JDBC 驱动中的 PGobject 类，用于单元测试。
 * 仅实现 {@code getValue()} 方法，满足 {@code AbstractResultSetMapper} 的反射调用。
 */
public class PGobject {

    private String value;
    private String type;

    public PGobject() {
    }

    public PGobject(String type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
