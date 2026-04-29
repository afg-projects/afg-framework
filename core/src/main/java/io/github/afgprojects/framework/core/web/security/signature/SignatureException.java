package io.github.afgprojects.framework.core.web.security.signature;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.model.exception.BusinessException;
import io.github.afgprojects.framework.core.model.exception.CommonErrorCode;

/**
 * 签名异常
 * <p>
 * 当签名验证失败时抛出，包含具体的错误原因。
 */
public class SignatureException extends BusinessException {

    private final SignatureErrorType errorType;

    /**
     * 创建签名异常
     *
     * @param errorType 错误类型
     */
    public SignatureException(@NonNull SignatureErrorType errorType) {
        super(CommonErrorCode.FORBIDDEN, errorType.getMessage());
        this.errorType = errorType;
    }

    /**
     * 创建签名异常
     *
     * @param errorType 错误类型
     * @param message   详细错误信息
     */
    public SignatureException(@NonNull SignatureErrorType errorType, @Nullable String message) {
        super(CommonErrorCode.FORBIDDEN, message != null ? message : errorType.getMessage());
        this.errorType = errorType;
    }

    /**
     * 创建签名异常
     *
     * @param errorType 错误类型
     * @param cause     原始异常
     */
    public SignatureException(@NonNull SignatureErrorType errorType, @Nullable Throwable cause) {
        super(CommonErrorCode.FORBIDDEN, errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    /**
     * 获取错误类型
     *
     * @return 错误类型
     */
    public SignatureErrorType getErrorType() {
        return errorType;
    }

    /**
     * 签名错误类型
     */
    public enum SignatureErrorType {
        /**
         * 缺少签名头
         */
        MISSING_SIGNATURE("缺少签名头 X-Signature"),

        /**
         * 缺少时间戳
         */
        MISSING_TIMESTAMP("缺少时间戳头 X-Timestamp"),

        /**
         * 缺少 nonce
         */
        MISSING_NONCE("缺少随机数头 X-Nonce"),

        /**
         * 时间戳格式错误
         */
        INVALID_TIMESTAMP("时间戳格式错误"),

        /**
         * 时间戳已过期
         */
        TIMESTAMP_EXPIRED("请求时间戳已过期"),

        /**
         * nonce 已被使用（重放攻击）
         */
        NONCE_REUSED("随机数已被使用，可能是重放攻击"),

        /**
         * 签名验证失败
         */
        INVALID_SIGNATURE("签名验证失败"),

        /**
         * 密钥不存在
         */
        KEY_NOT_FOUND("指定的密钥不存在"),

        /**
         * 密钥已禁用
         */
        KEY_DISABLED("指定的密钥已禁用"),

        /**
         * 签名算法不支持
         */
        UNSUPPORTED_ALGORITHM("不支持的签名算法");

        private final String message;

        SignatureErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
