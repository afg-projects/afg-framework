package io.github.afgprojects.framework.security.auth.datascope.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
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
public class SecUserDataScope extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "data_scope_id", nullable = false)
    private String dataScopeId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
