package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;

/**
 * SQL 注入防护过滤器
 */
public class SqlInjectionFilter extends AbstractSecurityFilter {

    private final SqlInjectionChecker checker;

    public SqlInjectionFilter() {
        this.checker = new SqlInjectionChecker();
    }

    public SqlInjectionFilter(@Nullable EnhancedInputSanitizer sanitizer) {
        this.checker = new SqlInjectionChecker(sanitizer);
    }

    @Override
    protected SecurityChecker getChecker() {
        return checker;
    }
}
