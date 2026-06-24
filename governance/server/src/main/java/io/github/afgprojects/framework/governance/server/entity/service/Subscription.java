package io.github.afgprojects.framework.governance.server.entity.service;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 订阅关系实体
 */
@Getter
@Setter
@AfEntity
@Entity
@Table(name = "gov_subscription")
public class Subscription extends BaseEntity {

    /**
     * 服务名称
     */
    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    /**
     * 实例ID
     */
    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId;

    /**
     * 订阅的配置键（JSON数组格式）
     */
    @Column(name = "subscribed_keys", columnDefinition = "TEXT")
    private String subscribedKeys;

    /**
     * 连接时间
     */
    @Column(name = "connected_at")
    private Instant connectedAt;

    /**
     * 最后活跃时间
     */
    @Column(name = "last_active")
    private Instant lastActive;

    /**
     * 状态：0-断开，1-连接
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;
}
