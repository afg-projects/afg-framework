package io.github.afgprojects.framework.data.core.tenant;

/**
 * 租户感知接口
 * <p>
 * 实现此接口的实体可以自动获取和设置租户ID
 */
public interface TenantAware {

    /**
     * 获取租户ID
     *
     * @return 租户ID
     */
    String getTenantId();

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    void setTenantId(String tenantId);
}
