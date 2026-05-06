package io.github.afgprojects.framework.data.core.exception;

/**
 * 数据权限异常
 * <p>
 * 当数据权限检查失败时抛出此异常。
 */
public class DataPermissionException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 权限范围类型
     */
    private final String scopeType;

    /**
     * 创建数据权限异常
     *
     * @param message 错误消息
     */
    public DataPermissionException(String message) {
        super(message);
        this.scopeType = null;
    }

    /**
     * 创建数据权限异常
     *
     * @param entityClass 实体类
     * @param scopeType   权限范围类型
     */
    public DataPermissionException(Class<?> entityClass, String scopeType) {
        super(String.format("Data permission denied for entity '%s' with scope '%s'",
                entityClass.getSimpleName(), scopeType),
                entityClass.getSimpleName());
        this.scopeType = scopeType;
    }

    /**
     * 创建数据权限异常
     *
     * @param message   错误消息
     * @param scopeType 权限范围类型
     */
    public DataPermissionException(String message, String scopeType) {
        super(message);
        this.scopeType = scopeType;
    }

    /**
     * 获取权限范围类型
     *
     * @return 权限范围类型，可能为 null
     */
    public String getScopeType() {
        return scopeType;
    }
}