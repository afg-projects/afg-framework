package io.github.afgprojects.framework.core.model.exception;

import java.io.Serial;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.exception.AfgException;
import lombok.Getter;

/**
 * 业务异常
 * 支持错误码和自定义消息，支持国际化
 */
@Getter
public class BusinessException extends AfgException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;
    private final String businessMessage;
    private final Object[] args;
    private final boolean customMessage;

    public BusinessException(String message) {
        super(CommonErrorCode.FAIL.getCode(), message);
        this.errorCode = CommonErrorCode.FAIL;
        this.businessMessage = message;
        this.args = null;
        this.customMessage = true;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
        this.businessMessage = errorCode.getMessage();
        this.args = null;
        this.customMessage = false;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode.getCode(), message);
        this.errorCode = errorCode;
        this.businessMessage = message;
        this.args = null;
        this.customMessage = true;
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode.getCode(), message, cause);
        this.errorCode = errorCode;
        this.businessMessage = message;
        this.args = null;
        this.customMessage = true;
    }

    /**
     * 创建带参数的业务异常（支持消息模板）
     *
     * @param errorCode 错误码
     * @param args 消息参数
     */
    public BusinessException(ErrorCode errorCode, Object[] args) {
        super(errorCode.getCode(), errorCode.getMessage(args, null));
        this.errorCode = errorCode;
        this.businessMessage = errorCode.getMessage(args, null);
        this.args = args;
        this.customMessage = false;
    }

    /**
     * 创建带参数和原因的异常
     *
     * @param errorCode 错误码
     * @param args 消息参数
     * @param cause 原因
     */
    public BusinessException(ErrorCode errorCode, Object[] args, Throwable cause) {
        super(errorCode.getCode(), errorCode.getMessage(args, null), cause);
        this.errorCode = errorCode;
        this.businessMessage = errorCode.getMessage(args, null);
        this.args = args;
        this.customMessage = false;
    }

    /**
     * 获取业务消息
     * @return 业务消息
     */
    @Override
    public String getMessage() {
        return businessMessage;
    }

    /**
     * 获取指定语言的业务消息
     * @param locale 语言
     * @return 国际化消息
     */
    @NonNull
    public String getMessage(@Nullable Locale locale) {
        // 如果是自定义消息，直接返回不进行国际化
        if (customMessage) {
            return businessMessage;
        }
        // 否则根据错误码获取国际化消息
        if (errorCode != null) {
            return errorCode.getMessage(args, locale);
        }
        return businessMessage;
    }

    /**
     * 获取错误码
     * @return 错误码
     */
    @Override
    public int getCode() {
        return errorCode != null ? errorCode.getCode() : super.getCode();
    }
}
