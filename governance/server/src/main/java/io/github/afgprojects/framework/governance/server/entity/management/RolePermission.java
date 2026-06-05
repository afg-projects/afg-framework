package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色权限关联表
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_role_permission", indexes = {
    @Index(name = "idx_role_permission_role", columnList = "role_id"),
    @Index(name = "idx_role_permission_permission", columnList = "permission_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_id", "permission_id"})
})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;
}
