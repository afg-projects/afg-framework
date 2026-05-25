package io.github.afgprojects.framework.data.core.dialect;

import org.jspecify.annotations.NonNull;

/**
 * openGauss 方言。
 * 继承 PostgreSQLDialect，覆盖 openGauss 特有的类型映射。
 */
public class OpenGaussDialect extends PostgreSQLDialect {

    @Override
    public @NonNull DatabaseType getDatabaseType() {
        return DatabaseType.OPENGAUSS;
    }
}
