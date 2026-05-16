package io.github.afgprojects.framework.security.auth.tenant.model;

import io.github.afgprojects.framework.security.core.tenant.TenantErrorCode;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 租户验证结果。
 *
 * <p>表示租户验证的结果，包含是否有效、错误码和错误消息。
 * 这是一个不可变类，通过工厂方法创建实例。
 *
 * @since 1.0.0
 */
public final class ValidationResult {

    private static final ValidationResult VALID = new ValidationResult(true, null, null);

    private final boolean valid;
    private final TenantErrorCode errorCode;
    private final String errorMessage;

    private ValidationResult(boolean valid, @Nullable TenantErrorCode errorCode, @Nullable String errorMessage) {
        this.valid = valid;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 创建有效的验证结果。
     *
     * @return 有效的验证结果实例
     */
    public static ValidationResult valid() {
        return VALID;
    }

    /**
     * 创建无效的验证结果。
     *
     * @param errorCode    错误码
     * @param errorMessage 错误消息
     * @return 无效的验证结果实例
     */
    public static ValidationResult invalid(@Nullable TenantErrorCode errorCode, @Nullable String errorMessage) {
        return new ValidationResult(false, errorCode, errorMessage);
    }

    /**
     * 判断验证是否通过。
     *
     * @return 如果验证通过返回 true
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码，验证通过时返回 null
     */
    @Nullable
    public TenantErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 获取错误消息。
     *
     * @return 错误消息，验证通过时返回 null
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValidationResult that = (ValidationResult) o;
        return valid == that.valid &&
            errorCode == that.errorCode &&
            Objects.equals(errorMessage, that.errorMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valid, errorCode, errorMessage);
    }

    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        }
        return "ValidationResult{" +
            "valid=false" +
            ", errorCode=" + errorCode +
            ", errorMessage='" + errorMessage + '\'' +
            '}';
    }
}
