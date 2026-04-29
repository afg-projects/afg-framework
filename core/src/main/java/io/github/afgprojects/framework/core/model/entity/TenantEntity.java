package io.github.afgprojects.framework.core.model.entity;

import java.io.Serial;

import lombok.Getter;
import lombok.Setter;

/**
 * 多租户实体
 */
@Getter
@Setter
@SuppressWarnings("PMD.UncommentedEmptyConstructor")
public class TenantEntity extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    protected String tenantId;
    protected String createdBy;
    protected String updatedBy;

    /**
     * 默认构造函数
     */
    protected TenantEntity() {}
}
