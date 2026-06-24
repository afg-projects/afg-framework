package io.github.afgprojects.framework.security.auth.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_role")
public class SecRole extends BaseEntity {

    @Column(name = "role_code", nullable = false, length = 50)
    private String roleCode;

    @Column(name = "role_name", nullable = false, length = 100)
    private String roleName;

    @Column(name = "role_type", nullable = false, length = 20)
    private String roleType = "CUSTOM";

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "description", length = 255)
    private String description;
}
