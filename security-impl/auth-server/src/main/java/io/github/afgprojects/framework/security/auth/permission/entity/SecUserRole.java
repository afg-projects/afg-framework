package io.github.afgprojects.framework.security.auth.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
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
public class SecUserRole extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "role_id", nullable = false)
    private String roleId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
