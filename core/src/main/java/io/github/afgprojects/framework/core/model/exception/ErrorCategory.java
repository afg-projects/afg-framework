package io.github.afgprojects.framework.core.model.exception;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.exception.ErrorCategory} 代替
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public enum ErrorCategory {
    BUSINESS("B"),
    SYSTEM("S"),
    NETWORK("N"),
    SECURITY("A");

    private final String prefix;

    ErrorCategory(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
