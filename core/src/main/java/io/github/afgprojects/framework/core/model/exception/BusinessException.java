package io.github.afgprojects.framework.core.model.exception;

import java.util.Locale;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @deprecated 使用 {@link io.github.afgprojects.framework.commons.exception.BusinessException} 代替
 */
@Deprecated(since = "1.1.0", forRemoval = true)
public class BusinessException extends io.github.afgprojects.framework.commons.exception.BusinessException {
    
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(io.github.afgprojects.framework.commons.exception.ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(io.github.afgprojects.framework.commons.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(io.github.afgprojects.framework.commons.exception.ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public BusinessException(io.github.afgprojects.framework.commons.exception.ErrorCode errorCode, Object[] args) {
        super(errorCode, args);
    }

    public BusinessException(io.github.afgprojects.framework.commons.exception.ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode, args, cause);
    }

    @Override
    public @NonNull String getMessage(@Nullable Locale locale) {
        return super.getMessage(locale);
    }
}
