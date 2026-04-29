package io.github.afgprojects.framework.data.core.entity;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.data.core.tenant.TenantAware;

/**
 * 租户实体类
 * <p>
 * 支持多租户的实体，继承 BaseEntity，增加租户ID字段
 *
 * @param <ID> 主键类型
 */
public abstract class TenantEntity<ID> extends BaseEntity<ID> implements TenantAware {

    /**
     * 租户ID
     */
    protected @Nullable String tenantId;

    @Override
    public @Nullable String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(@Nullable String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", tenantId='" + tenantId + "'}";
    }
}