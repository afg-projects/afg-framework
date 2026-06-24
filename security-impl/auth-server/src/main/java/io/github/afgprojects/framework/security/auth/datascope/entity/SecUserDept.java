package io.github.afgprojects.framework.security.auth.datascope.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户-部门关联实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_user_dept")
public class SecUserDept extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;

    @Column(name = "dept_id", nullable = false)
    private String deptId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;
}
