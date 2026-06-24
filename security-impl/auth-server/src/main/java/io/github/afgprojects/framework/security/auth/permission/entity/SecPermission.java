package io.github.afgprojects.framework.security.auth.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 权限实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_permission")
public class SecPermission extends BaseEntity {

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;

    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    @Column(name = "resource_id")
    private String resourceId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
