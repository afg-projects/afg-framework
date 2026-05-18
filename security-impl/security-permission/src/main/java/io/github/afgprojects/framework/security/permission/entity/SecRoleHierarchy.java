package io.github.afgprojects.framework.security.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
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
public class SecRoleHierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "parent_role_id", nullable = false)
    private Long parentRoleId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
