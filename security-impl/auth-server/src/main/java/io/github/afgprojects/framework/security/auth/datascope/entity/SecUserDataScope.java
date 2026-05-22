package io.github.afgprojects.framework.security.auth.datascope.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户数据范围关联实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_user_data_scope")
public class SecUserDataScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "data_scope_id", nullable = false)
    private Long dataScopeId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
