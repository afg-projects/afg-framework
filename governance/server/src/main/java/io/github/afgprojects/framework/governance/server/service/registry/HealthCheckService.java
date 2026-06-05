package io.github.afgprojects.framework.governance.server.service.registry;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.governance.server.entity.service.ServiceInstance;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static io.github.afgprojects.framework.data.core.condition.Conditions.builder;

/**
 * 健康检查服务
 * <p>
 * 定期检查服务实例的心跳状态，自动标记不健康和已下线的实例
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final DataManager dataManager;

    @Value("${governance.health.heartbeat-timeout:30000}")
    private long heartbeatTimeout;

    @Value("${governance.health.eviction-timeout:90000}")
    private long evictionTimeout;

    /**
     * 定期检查服务实例健康状态
     * <p>
     * 每5秒执行一次：
     * - 超过 evictionTimeout 未心跳 → 标记为已删除
     * - 超过 heartbeatTimeout 未心跳 → 标记为不健康（status=0）
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void checkHealth() {
        log.debug("Starting health check for service instances");

        Instant now = Instant.now();
        Instant evictionThreshold = now.minus(evictionTimeout, ChronoUnit.MILLIS);
        Instant timeoutThreshold = now.minus(heartbeatTimeout, ChronoUnit.MILLIS);

        // 查询所有未删除的实例
        List<ServiceInstance> instances = dataManager.entity(ServiceInstance.class)
                .query()
                .where(builder(ServiceInstance.class)
                        .eq(ServiceInstance::isDeleted, false)
                        .build())
                .list();

        int evictedCount = 0;
        int unhealthyCount = 0;

        for (ServiceInstance instance : instances) {
            Instant lastHeartbeat = instance.getLastHeartbeat();

            // 如果没有心跳记录，跳过
            if (lastHeartbeat == null) {
                continue;
            }

            // 超过驱逐时间 → 标记为已删除
            if (lastHeartbeat.isBefore(evictionThreshold)) {
                instance.setDeleted(true);
                instance.setStatus(0);
                dataManager.save(ServiceInstance.class, instance);
                evictedCount++;
                log.info("Service instance evicted: instanceId={}, serviceId={}, lastHeartbeat={}",
                        instance.getInstanceId(), instance.getServiceId(), lastHeartbeat);
                continue;
            }

            // 超过心跳超时时间 → 标记为不健康
            if (lastHeartbeat.isBefore(timeoutThreshold) && instance.getStatus() == 1) {
                instance.setStatus(0);
                dataManager.save(ServiceInstance.class, instance);
                unhealthyCount++;
                log.warn("Service instance marked unhealthy: instanceId={}, serviceId={}, lastHeartbeat={}",
                        instance.getInstanceId(), instance.getServiceId(), lastHeartbeat);
            }
        }

        if (evictedCount > 0 || unhealthyCount > 0) {
            log.info("Health check completed: evicted={}, unhealthy={}, total={}",
                    evictedCount, unhealthyCount, instances.size());
        }
    }
}
