package io.github.afgprojects.framework.security.datascope.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 部门实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_dept")
public class SecDept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_code", nullable = false, length = 50)
    private String deptCode;

    @Column(name = "dept_name", nullable = false, length = 100)
    private String deptName;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "leader_user_id", length = 100)
    private String leaderUserId;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(name = "status")
    private Integer status = 1;
}
