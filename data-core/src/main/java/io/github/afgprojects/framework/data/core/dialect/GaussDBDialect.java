package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * GaussDB 方言。
 * 继承 PostgreSQLDialect，覆盖 GaussDB 特有的类型映射。
 */
public class GaussDBDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.GAUSSDB;
    }

    @Override
    protected void configureSqlTypeMap(java.util.Map<Class<?>, String> map) {
        super.configureSqlTypeMap(map);
        map.put(String.class, "VARCHAR2(255)");
        map.put(Object.class, "VARCHAR2(255)");
    }
}
