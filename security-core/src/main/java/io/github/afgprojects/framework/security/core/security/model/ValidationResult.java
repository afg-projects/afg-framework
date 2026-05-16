package io.github.afgprojects.framework.security.core.security.model;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 验证结果。
 *
 * <p>用于表示安全策略验证的结果，包含验证状态、消息和错误列表。
 *
 * @since 1.0.0
 */
public final class ValidationResult {

    private final boolean valid;

    @Nullable
    private final String message;

    @Nullable
    private final List<String> errors;

    /**
     * 私有构造函数。
     *
     * @param valid  验证是否通过
     * @param message 验证消息
     * @param errors  错误列表
     */
    private ValidationResult(boolean valid, @Nullable String message, @Nullable List<String> errors) {
        this.valid = valid;
        this.message = message;
        this.errors = errors != null ? new ArrayList<>(errors) : null;
    }

    /**
     * 创建验证通过的结果。
     *
     * @return 验证通过的结果
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null, null);
    }

    /**
     * 创建验证失败的结果。
     *
     * @param message 失败消息
     * @return 验证失败的结果
     */
    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, message, null);
    }

    /**
     * 创建包含多个错误的验证失败结果。
     *
     * @param errors 错误列表
     * @return 验证失败的结果
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, null, errors);
    }

    /**
     * 判断验证是否通过。
     *
     * @return 如果验证通过则返回 true
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 获取验证消息。
     *
     * @return 验证消息，可能为 null
     */
    @Nullable
    public String getMessage() {
        return message;
    }

    /**
     * 获取错误列表。
     *
     * @return 错误列表的不可变视图，可能为 null
     */
    @Nullable
    public List<String> getErrors() {
        return errors != null ? Collections.unmodifiableList(errors) : null;
    }
}
