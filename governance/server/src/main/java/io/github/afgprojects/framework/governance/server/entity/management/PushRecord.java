package io.github.afgprojects.framework.governance.server.entity.management;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 推送记录实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_push_record", indexes = {
    @Index(name = "idx_push_config_item", columnList = "config_item_id"),
    @Index(name = "idx_push_instance", columnList = "instance_id"),
    @Index(name = "idx_push_status", columnList = "push_status")
})
public class PushRecord extends BaseEntity {

    @Column(name = "config_item_id", nullable = false)
    private String configItemId;

    @Column(name = "instance_id", nullable = false)
    private String instanceId;

    @Column(name = "push_status", nullable = false, length = 20)
    private String pushStatus;

    @Column(name = "push_time")
    private Instant pushTime;

    @Column(name = "ack_time")
    private Instant ackTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
