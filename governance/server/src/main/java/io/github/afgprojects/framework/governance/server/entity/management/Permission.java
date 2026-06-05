package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 权限实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_permission", indexes = {
    @Index(name = "idx_permission_code", columnList = "code", unique = true)
})
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 100)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "resource_type", length = 20)
    private String resourceType;

    @Column(name = "created_at")
    private Instant createdAt;
}
