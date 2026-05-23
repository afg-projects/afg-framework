package io.github.afgprojects.framework.core.exception;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.exception.AfgException} 代替
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public abstract class AfgException extends io.github.afgprojects.framework.commons.exception.AfgException {
    protected AfgException(int code, String message) {
        super(code, message);
    }

    protected AfgException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
