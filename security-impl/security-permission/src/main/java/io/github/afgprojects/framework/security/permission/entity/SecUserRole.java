package io.github.afgprojects.framework.security.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户-角色关联实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_user_role")
public class SecUserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
