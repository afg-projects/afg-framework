package io.github.afgprojects.framework.data.core.exception;

/**
 * 实体未找到异常
 * <p>
 * 当根据 ID 或条件查询实体但未找到时抛出此异常。
 * <p>
 * 使用示例：
 * <pre>
 * User user = dataManager.entity(User.class)
 *     .findById(1L)
 *     .orElseThrow(() -> new EntityNotFoundException(User.class, 1L));
 * </pre>
 */
public class EntityNotFoundException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 实体 ID
     */
    private final Object entityId;

    /**
     * 创建实体未找到异常
     *
     * @param entityClass 实体类
     * @param id          实体 ID
     */
    public EntityNotFoundException(Class<?> entityClass, Object id) {
        super(String.format("Entity '%s' with id '%s' not found",
                entityClass.getSimpleName(), id),
                entityClass.getSimpleName());
        this.entityId = id;
    }

    /**
     * 创建实体未找到异常
     *
     * @param entityClassName 实体类名
     * @param id              实体 ID
     */
    public EntityNotFoundException(String entityClassName, Object id) {
        super(String.format("Entity '%s' with id '%s' not found", entityClassName, id),
                entityClassName);
        this.entityId = id;
    }

    /**
     * 创建实体未找到异常（自定义消息）
     *
     * @param message         自定义消息
     * @param entityClassName 实体类名
     * @param id              实体 ID
     */
    public EntityNotFoundException(String message, String entityClassName, Object id) {
        super(message, entityClassName);
        this.entityId = id;
    }

    /**
     * 获取实体 ID
     *
     * @return 实体 ID
     */
    public Object getEntityId() {
        return entityId;
    }
}