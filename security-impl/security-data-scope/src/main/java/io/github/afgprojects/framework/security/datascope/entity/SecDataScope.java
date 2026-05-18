package io.github.afgprojects.framework.security.datascope.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.scope.DataScopeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 数据范围定义实体
 */
@Getter
@Setter
@AfEntity
@Table(name = "sec_data_scope")
public class SecDataScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scope_code", nullable = false, length = 50)
    private String scopeCode;

    @Column(name = "scope_name", nullable = false, length = 100)
    private String scopeName;

    @Column(name = "scope_type", nullable = false, length = 20)
    private String scopeType;

    @Column(name = "resource_code", length = 100)
    private String resourceCode;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    /**
     * 获取数据范围类型枚举
     */
    public DataScopeType getScopeTypeEnum() {
        try {
            return DataScopeType.valueOf(scopeType);
        } catch (IllegalArgumentException e) {
            return DataScopeType.ALL;
        }
    }
}
