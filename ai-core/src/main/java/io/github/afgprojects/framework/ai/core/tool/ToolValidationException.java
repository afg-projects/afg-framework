package io.github.afgprojects.framework.ai.core.tool;

import org.jspecify.annotations.Nullable;

/**
 * 工具校验异常。
 *
 * <p>当工具输入参数校验失败时抛出。
 *
 * @since 1.0.0
 */
public class ToolValidationException extends RuntimeException {

    private final String fieldName;
    private final Object invalidValue;

    /**
     * 创建校验异常。
     *
     * @param message 错误消息
     */
    public ToolValidationException(@Nullable String message) {
        super(message);
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * 创建校验异常。
     *
     * @param message 错误消息
     * @param cause   原因
     */
    public ToolValidationException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * 创建字段校验异常。
     *
     * @param fieldName    字段名
     * @param invalidValue 无效值
     * @param message      错误消息
     */
    public ToolValidationException(
            @Nullable String fieldName,
            @Nullable Object invalidValue,
            @Nullable String message) {
        super(message);
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    /**
     * 获取校验失败的字段名。
     *
     * @return 字段名
     */
    @Nullable
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 获取校验失败的值。
     *
     * @return 无效值
     */
    @Nullable
    public Object getInvalidValue() {
        return invalidValue;
    }

    /**
     * 创建必填字段缺失异常。
     *
     * @param fieldName 字段名
     * @return 校验异常
     */
    public static ToolValidationException required(@Nullable String fieldName) {
        return new ToolValidationException(fieldName, null, "Field '" + fieldName + "' is required");
    }

    /**
     * 创建字段格式错误异常。
     *
     * @param fieldName    字段名
     * @param invalidValue 无效值
     * @param expectedType 期望类型
     * @return 校验异常
     */
    public static ToolValidationException invalidFormat(
            @Nullable String fieldName,
            @Nullable Object invalidValue,
            @Nullable String expectedType) {
        return new ToolValidationException(
            fieldName,
            invalidValue,
            "Field '" + fieldName + "' has invalid format, expected: " + expectedType
        );
    }

    /**
     * 创建字段值范围错误异常。
     *
     * @param fieldName 字段名
     * @param value     无效值
     * @param min       最小值
     * @param max       最大值
     * @return 校验异常
     */
    public static ToolValidationException outOfRange(
            @Nullable String fieldName,
            @Nullable Object value,
            @Nullable Object min,
            @Nullable Object max) {
        return new ToolValidationException(
            fieldName,
            value,
            "Field '" + fieldName + "' value " + value + " is out of range [" + min + ", " + max + "]"
        );
    }
}
