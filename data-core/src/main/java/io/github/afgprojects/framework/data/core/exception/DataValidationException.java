package io.github.afgprojects.framework.data.core.exception;

/**
 * 数据验证异常
 * <p>
 * 当实体数据验证失败时抛出此异常。
 */
public class DataValidationException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 验证失败的字段名
     */
    private final String fieldName;

    /**
     * 创建数据验证异常
     *
     * @param message 错误消息
     */
    public DataValidationException(String message) {
        super(message);
        this.fieldName = null;
    }

    /**
     * 创建数据验证异常
     *
     * @param fieldName 字段名
     * @param message   错误消息
     */
    public DataValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    /**
     * 创建数据验证异常
     *
     * @param entityClass 实体类
     * @param fieldName   字段名
     * @param message     错误消息
     */
    public DataValidationException(Class<?> entityClass, String fieldName, String message) {
        super(String.format("Validation failed for field '%s' in entity '%s': %s",
                fieldName, entityClass.getSimpleName(), message),
                entityClass.getSimpleName());
        this.fieldName = fieldName;
    }

    /**
     * 获取验证失败的字段名
     *
     * @return 字段名，可能为 null
     */
    public String getFieldName() {
        return fieldName;
    }
}