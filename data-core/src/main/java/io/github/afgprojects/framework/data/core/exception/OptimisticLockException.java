package io.github.afgprojects.framework.data.core.exception;

/**
 * 乐观锁异常
 * <p>
 * 当乐观锁更新失败时抛出此异常（即 UPDATE 行数为 0，表示版本冲突）。
 * </p>
 */
public class OptimisticLockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 实体类名
     */
    private final String entityClassName;

    /**
     * 实体 ID
     */
    private final Object entityId;

    /**
     * 期望的版本号
     */
    private final long expectedVersion;

    /**
     * 构造乐观锁异常
     *
     * @param entityClassName  实体类名
     * @param entityId         实体 ID
     * @param expectedVersion  期望的版本号
     */
    public OptimisticLockException(String entityClassName, Object entityId, long expectedVersion) {
        super(String.format("Optimistic lock conflict: entity=%s, id=%s, expectedVersion=%d",
            entityClassName, entityId, expectedVersion));
        this.entityClassName = entityClassName;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
    }

    /**
     * 构造乐观锁异常（带自定义消息）
     *
     * @param message          自定义消息
     * @param entityClassName  实体类名
     * @param entityId         实体 ID
     * @param expectedVersion  期望的版本号
     */
    public OptimisticLockException(String message, String entityClassName, Object entityId, long expectedVersion) {
        super(message);
        this.entityClassName = entityClassName;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
    }

    /**
     * 获取实体类名
     *
     * @return 实体类名
     */
    public String getEntityClassName() {
        return entityClassName;
    }

    /**
     * 获取实体 ID
     *
     * @return 实体 ID
     */
    public Object getEntityId() {
        return entityId;
    }

    /**
     * 获取期望的版本号
     *
     * @return 期望的版本号
     */
    public long getExpectedVersion() {
        return expectedVersion;
    }
}