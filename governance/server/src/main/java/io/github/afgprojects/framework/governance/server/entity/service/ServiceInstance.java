package io.github.afgprojects.framework.governance.server.entity.service;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.FullEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

/**
 * 服务实例实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AfEntity
@Entity
@Table(name = "gov_service_instance")
public class ServiceInstance extends FullEntity {

    /**
     * 服务ID（关联 ServiceRegistry）
     */
    @Column(name = "service_id", nullable = false)
    private Long serviceId;

    /**
     * 实例ID（唯一标识）
     */
    @Column(name = "instance_id", nullable = false, length = 100, unique = true)
    private String instanceId;

    /**
     * 主机地址
     */
    @Column(name = "host", nullable = false, length = 100)
    private String host;

    /**
     * 端口号
     */
    @Column(name = "port", nullable = false)
    private Integer port;

    /**
     * 协议：http、grpc、dubbo等
     */
    @Column(name = "protocol", length = 20)
    private String protocol;

    /**
     * 权重（负载均衡用）
     */
    @Column(name = "weight")
    private Integer weight = 100;

    /**
     * 元数据（JSON格式）
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    /**
     * 状态：0-下线，1-在线
     */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /**
     * 最后心跳时间
     */
    @Column(name = "last_heartbeat")
    private Instant lastHeartbeat;
}
