package io.github.afgprojects.framework.governance.server.entity.service;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务注册实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_service_registry")
public class ServiceRegistry extends FullEntity {

    /**
     * 服务编码
     */
    @Column(name = "code", nullable = false, length = 100, unique = true)
    private String code;

    /**
     * 服务名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 服务描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 分组名称
     */
    @Column(name = "group_name", length = 100)
    private String groupName;

    /**
     * 服务版本
     */
    @Column(name = "service_version", length = 50)
    private String serviceVersion;

    /**
     * 元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 状态：0-禁用，1-启用
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
