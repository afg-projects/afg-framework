package io.github.afgprojects.framework.data.core.exception;

/**
 * 实体映射异常
 * <p>
 * 当实体与数据库记录之间的映射失败时抛出此异常。
 */
public class EntityMappingException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 字段名
     */
    private final String fieldName;

    /**
     * 创建实体映射异常
     *
     * @param message 错误消息
     */
    public EntityMappingException(String message) {
        super(message);
        this.fieldName = null;
    }

    /**
     * 创建实体映射异常
     *
     * @param entityClass 实体类
     * @param fieldName   字段名
     * @param message     错误消息
     */
    public EntityMappingException(Class<?> entityClass, String fieldName, String message) {
        super(String.format("Mapping error for field '%s' in entity '%s': %s",
                fieldName, entityClass.getSimpleName(), message),
                entityClass.getSimpleName());
        this.fieldName = fieldName;
    }

    /**
     * 创建实体映射异常
     *
     * @param message   错误消息
     * @param fieldName 字段名
     */
    public EntityMappingException(String message, String fieldName) {
        super(message);
        this.fieldName = fieldName;
    }

    /**
     * 创建实体映射异常
     *
     * @param message   错误消息
     * @param fieldName 字段名
     * @param cause     原因
     */
    public EntityMappingException(String message, String fieldName, Throwable cause) {
        super(message, cause);
        this.fieldName = fieldName;
    }

    /**
     * 获取字段名
     *
     * @return 字段名，可能为 null
     */
    public String getFieldName() {
        return fieldName;
    }
}