package io.github.afgprojects.framework.governance.server.entity.user;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户角色关联表
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_user_role", indexes = {
    @Index(name = "idx_user_role_user", columnList = "user_id"),
    @Index(name = "idx_user_role_role", columnList = "role_id")
})
public class GovUserRole extends TenantEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;
}
