package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSanitizer;

/**
 * SQL 注入安全检查器
 */
public class SqlInjectionChecker implements SecurityChecker {

    private final @Nullable EnhancedInputSanitizer enhancedSanitizer;

    public SqlInjectionChecker() {
        this.enhancedSanitizer = null;
    }

    public SqlInjectionChecker(@Nullable EnhancedInputSanitizer sanitizer) {
        this.enhancedSanitizer = sanitizer;
    }

    @Override
    public String getName() {
        return "SQL_INJECTION";
    }

    @Override
    public boolean containsThreat(String input) {
        if (enhancedSanitizer != null) {
            return enhancedSanitizer.containsSqlInjection(input);
        }
        return InputSanitizer.containsSqlInjection(input);
    }
}
