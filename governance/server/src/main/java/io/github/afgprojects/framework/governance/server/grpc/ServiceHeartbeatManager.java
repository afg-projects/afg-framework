package io.github.afgprojects.framework.governance.server.grpc;

import io.github.afgprojects.framework.governance.server.service.registry.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service heartbeat manager for gRPC streaming.
 * Manages heartbeat processing and caching for service instances.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ServiceHeartbeatManager {

    private final ServiceRegistryService registryService;

    /**
     * Cache of instance ID to last heartbeat timestamp.
     */
    private final ConcurrentHashMap<String, Long> heartbeatCache = new ConcurrentHashMap<>();

    /**
     * Process a heartbeat from a service instance.
     * Updates the registry and caches the timestamp.
     *
     * @param instanceId the instance ID
     */
    public void processHeartbeat(String instanceId) {
        try {
            registryService.heartbeat(instanceId);
            heartbeatCache.put(instanceId, System.currentTimeMillis());
            log.debug("Processed heartbeat for instance: {}", instanceId);
        } catch (Exception e) {
            log.error("Failed to process heartbeat for instance: {}", instanceId, e);
        }
    }

    /**
     * Get the last heartbeat timestamp for an instance.
     *
     * @param instanceId the instance ID
     * @return the last heartbeat timestamp, or null if not cached
     */
    public Long getLastHeartbeat(String instanceId) {
        return heartbeatCache.get(instanceId);
    }
}
