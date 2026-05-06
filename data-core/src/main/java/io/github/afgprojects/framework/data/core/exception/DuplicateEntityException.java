package io.github.afgprojects.framework.data.core.exception;

/**
 * 实体重复异常
 * <p>
 * 当插入或更新实体时违反唯一约束时抛出此异常。
 * <p>
 * 使用示例：
 * <pre>
 * if (userRepository.existsByUsername(username)) {
 *     throw new DuplicateEntityException(User.class, "username", username);
 * }
 * </pre>
 */
public class DuplicateEntityException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 冲突的字段名
     */
    private final String fieldName;

    /**
     * 冲突的字段值
     */
    private final Object fieldValue;

    /**
     * 创建实体重复异常
     *
     * @param entityClass 实体类
     * @param fieldName   冲突的字段名
     * @param fieldValue  冲突的字段值
     */
    public DuplicateEntityException(Class<?> entityClass, String fieldName, Object fieldValue) {
        super(String.format("Duplicate '%s' with value '%s' already exists in entity '%s'",
                fieldName, fieldValue, entityClass.getSimpleName()),
                entityClass.getSimpleName());
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * 创建实体重复异常
     *
     * @param entityClassName 实体类名
     * @param fieldName       冲突的字段名
     * @param fieldValue      冲突的字段值
     */
    public DuplicateEntityException(String entityClassName, String fieldName, Object fieldValue) {
        super(String.format("Duplicate '%s' with value '%s' already exists in entity '%s'",
                fieldName, fieldValue, entityClassName),
                entityClassName);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * 创建实体重复异常（自定义消息）
     *
     * @param message         自定义消息
     * @param entityClassName 实体类名
     * @param fieldName       冲突的字段名
     * @param fieldValue      冲突的字段值
     */
    public DuplicateEntityException(String message, String entityClassName, String fieldName, Object fieldValue) {
        super(message, entityClassName);
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    /**
     * 获取冲突的字段名
     *
     * @return 字段名
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * 获取冲突的字段值
     *
     * @return 字段值
     */
    public Object getFieldValue() {
        return fieldValue;
    }
}