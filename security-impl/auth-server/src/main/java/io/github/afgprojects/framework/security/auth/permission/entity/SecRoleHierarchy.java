package io.github.afgprojects.framework.security.auth.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色继承关系实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_role_hierarchy")
public class SecRoleHierarchy extends BaseEntity {

    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(name = "parent_role_id", nullable = false)
    private String parentRoleId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
