package io.github.afgprojects.framework.data.core.entity;

import java.time.LocalDateTime;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.data.core.tenant.TenantAware;

/**
 * 完整实体类
 * <p>
 * 包含所有特性的实体：租户ID、软删除、乐观锁、审计字段
 *
 * @param <ID> 主键类型
 */
public abstract class FullEntity<ID> extends BaseEntity<ID>
        implements TenantAware, SoftDeletable, Versioned {

    /**
     * 租户ID
     */
    protected @Nullable String tenantId;

    /**
     * 删除标记
     */
    protected boolean deleted = false;

    /**
     * 版本号（乐观锁）
     */
    protected long version = 0L;

    /**
     * 创建人ID
     */
    protected @Nullable String createBy;

    /**
     * 更新人ID
     */
    protected @Nullable String updateBy;

    // ========== TenantAware ==========

    @Override
    public @Nullable String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(@Nullable String tenantId) {
        this.tenantId = tenantId;
    }

    // ========== SoftDeletable ==========

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * 标记为已删除
     */
    public void markDeleted() {
        this.deleted = true;
    }

    /**
     * 恢复删除
     */
    public void restore() {
        this.deleted = false;
    }

    // ========== Versioned ==========

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    // ========== Audit Fields ==========

    /**
     * 获取创建人ID
     *
     * @return 创建人ID
     */
    public @Nullable String getCreateBy() {
        return createBy;
    }

    /**
     * 设置创建人ID
     *
     * @param createBy 创建人ID
     */
    public void setCreateBy(@Nullable String createBy) {
        this.createBy = createBy;
    }

    /**
     * 获取更新人ID
     *
     * @return 更新人ID
     */
    public @Nullable String getUpdateBy() {
        return updateBy;
    }

    /**
     * 设置更新人ID
     *
     * @param updateBy 更新人ID
     */
    public void setUpdateBy(@Nullable String updateBy) {
        this.updateBy = updateBy;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "{id=" + id
                + ", tenantId='" + tenantId + '\''
                + ", deleted=" + deleted
                + ", version=" + version
                + '}';
    }
}