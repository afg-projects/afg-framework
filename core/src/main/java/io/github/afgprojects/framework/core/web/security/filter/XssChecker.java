package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;
import io.github.afgprojects.framework.core.web.security.sanitizer.InputSanitizer;

/**
 * XSS 安全检查器
 */
public class XssChecker implements SecurityChecker {

    private final @Nullable EnhancedInputSanitizer enhancedSanitizer;

    public XssChecker() {
        this.enhancedSanitizer = null;
    }

    public XssChecker(@Nullable EnhancedInputSanitizer sanitizer) {
        this.enhancedSanitizer = sanitizer;
    }

    @Override
    public String getName() {
        return "XSS";
    }

    @Override
    public boolean containsThreat(String input) {
        if (enhancedSanitizer != null) {
            return enhancedSanitizer.containsXss(input);
        }
        return InputSanitizer.containsXss(input);
    }
}
