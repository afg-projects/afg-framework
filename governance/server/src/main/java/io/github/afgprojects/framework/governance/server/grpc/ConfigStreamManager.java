package io.github.afgprojects.framework.governance.server.grpc;

import io.github.afgprojects.framework.governance.proto.ChangeType;
import io.github.afgprojects.framework.governance.proto.ConfigChangeNotification;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration stream manager for managing gRPC subscriptions.
 * Handles registration, removal, and notification of config changes to subscribed services.
 */
@Slf4j
@Component
public class ConfigStreamManager {

    /**
     * Service name -> list of subscription streams
     */
    private final Map<String, CopyOnWriteArrayList<StreamObserver<ConfigChangeNotification>>> subscriptions = new ConcurrentHashMap<>();

    /**
     * Register a subscription stream for a service.
     *
     * @param serviceName the service name
     * @param observer    the stream observer
     */
    public void registerStream(String serviceName, StreamObserver<ConfigChangeNotification> observer) {
        subscriptions.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>()).add(observer);
        log.info("Registered config subscription for service: {}", serviceName);
    }

    /**
     * Remove a subscription stream for a service.
     *
     * @param serviceName the service name
     * @param observer    the stream observer
     */
    public void removeStream(String serviceName, StreamObserver<ConfigChangeNotification> observer) {
        if (subscriptions.containsKey(serviceName)) {
            subscriptions.get(serviceName).remove(observer);
            log.info("Removed config subscription for service: {}", serviceName);
        }
    }

    /**
     * Push a configuration change notification to all subscribed services.
     *
     * @param key        the configuration key
     * @param value      the configuration value
     * @param changeType the change type (CREATE/UPDATE/DELETE)
     */
    public void pushConfigChange(String key, String value, ChangeType changeType) {
        ConfigChangeNotification notification = ConfigChangeNotification.newBuilder()
            .setKey(key)
            .setValue(value != null ? value : "")
            .setChangeType(changeType)
            .setTimestamp(System.currentTimeMillis())
            .setSource("governance-server")
            .build();

        subscriptions.forEach((serviceName, observers) -> {
            for (StreamObserver<ConfigChangeNotification> observer : observers) {
                try {
                    observer.onNext(notification);
                } catch (Exception e) {
                    log.warn("Failed to push config change to service: {}", serviceName, e);
                    observers.remove(observer);
                }
            }
        });

        log.info("Pushed config change: key={}, type={}", key, changeType);
    }

    /**
     * Get the total number of active subscriptions.
     *
     * @return the subscription count
     */
    public int getSubscriptionCount() {
        return subscriptions.values().stream()
            .mapToInt(CopyOnWriteArrayList::size)
            .sum();
    }
}
