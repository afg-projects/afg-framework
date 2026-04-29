package io.github.afgprojects.framework.core.web.security.filter;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.web.security.sanitizer.EnhancedInputSanitizer;

/**
 * XSS 防护过滤器
 */
public class XssFilter extends AbstractSecurityFilter {

    private final XssChecker checker;

    public XssFilter() {
        this.checker = new XssChecker();
    }

    public XssFilter(@Nullable EnhancedInputSanitizer sanitizer) {
        this.checker = new XssChecker(sanitizer);
    }

    @Override
    protected SecurityChecker getChecker() {
        return checker;
    }
}
