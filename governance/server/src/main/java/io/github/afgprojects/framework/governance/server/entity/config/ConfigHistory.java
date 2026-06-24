package io.github.afgprojects.framework.governance.server.entity.config;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_config_history", indexes = {
    @Index(name = "idx_config_history_item", columnList = "item_id"),
    @Index(name = "idx_config_history_operator", columnList = "operator_id"),
    @Index(name = "idx_config_history_time", columnList = "created_at")
})
public class ConfigHistory extends TenantEntity {

    @Column(name = "item_id", nullable = false)
    private String itemId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "operator_name", length = 50)
    private String operatorName;
}
