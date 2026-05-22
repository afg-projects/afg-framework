package io.github.afgprojects.framework.security.auth.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色-权限关联实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_role_permission")
public class SecRolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
