package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * 达梦数据库方言。
 * 兼容 Oracle 语法，继承 OracleDialect，仅覆盖差异部分。
 */
public class DmDialect extends OracleDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.DM;
    }

    @Override
    public @NonNull String getAutoIncrementSyntax() {
        return "IDENTITY";
    }

    @Override
    protected void configureSqlTypeMap(java.util.Map<Class<?>, String> map) {
        // 达梦数据类型与 Oracle 有细微差异
        map.put(String.class, "VARCHAR2(255)");
        map.put(Integer.class, "INTEGER");
        map.put(int.class, "INTEGER");
        map.put(Long.class, "BIGINT");
        map.put(long.class, "BIGINT");
        map.put(Double.class, "DOUBLE");
        map.put(double.class, "DOUBLE");
        map.put(Float.class, "FLOAT");
        map.put(float.class, "FLOAT");
        map.put(Boolean.class, "BIT");
        map.put(boolean.class, "BIT");
        map.put(java.math.BigDecimal.class, "DECIMAL(19,4)");
        map.put(byte[].class, "BLOB");
        map.put(Object.class, "VARCHAR2(255)");
    }
}