package io.github.afgprojects.framework.governance.server.entity.service;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

/**
 * 订阅关系实体
 */
@Data
@AfEntity
@Entity
@Table(name = "gov_subscription")
public class Subscription {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

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

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Instant updatedAt;
}
