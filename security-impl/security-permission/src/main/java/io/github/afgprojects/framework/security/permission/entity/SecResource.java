package io.github.afgprojects.framework.security.permission.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 资源实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_resource")
public class SecResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_code", nullable = false, length = 100)
    private String resourceCode;

    @Column(name = "resource_name", nullable = false, length = 100)
    private String resourceName;

    @Column(name = "resource_type", nullable = false, length = 20)
    private String resourceType;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "status")
    private Integer status = 1;
}
