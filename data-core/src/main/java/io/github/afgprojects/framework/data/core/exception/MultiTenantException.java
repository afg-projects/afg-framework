package io.github.afgprojects.framework.data.core.exception;

/**
 * 多租户异常
 * <p>
 * 当多租户相关操作失败时抛出此异常。
 */
public class MultiTenantException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID
     */
    private final String tenantId;

    /**
     * 创建多租户异常
     *
     * @param message 错误消息
     */
    public MultiTenantException(String message) {
        super(message);
        this.tenantId = null;
    }

    /**
     * 创建多租户异常
     *
     * @param message  错误消息
     * @param tenantId 租户 ID
     */
    public MultiTenantException(String message, String tenantId) {
        super(message);
        this.tenantId = tenantId;
    }

    /**
     * 创建多租户异常
     *
     * @param entityClass 实体类
     * @param tenantId    租户 ID
     * @param message     错误消息
     */
    public MultiTenantException(Class<?> entityClass, String tenantId, String message) {
        super(String.format("Multi-tenant error for entity '%s' with tenant '%s': %s",
                entityClass.getSimpleName(), tenantId, message),
                entityClass.getSimpleName());
        this.tenantId = tenantId;
    }

    /**
     * 获取租户 ID
     *
     * @return 租户 ID，可能为 null
     */
    public String getTenantId() {
        return tenantId;
    }
}