package io.github.afgprojects.framework.governance.server.entity.config;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import io.github.afgprojects.framework.data.core.entity.Versioned;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_config_value", indexes = {
    @Index(name = "idx_config_value_tenant", columnList = "tenant_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_config_value_item", columnNames = {"item_id"})
})
public class ConfigValue extends TenantEntity implements Versioned {

    @Column(name = "environment_id")
    private Long environmentId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "config_value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "value_type", length = 20)
    private String valueType;

    @Column(name = "version")
    private Integer version = 0;
}
