package io.github.afgprojects.framework.integration.governance.client.config;

import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import io.github.afgprojects.framework.integration.governance.api.*;
import io.github.afgprojects.framework.integration.governance.client.common.GovernanceChannelManager;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 配置中心客户端
 * <p>
 * 提供配置获取、发布和订阅功能，支持断线重连
 *
 * @author afg-projects
 */
@Slf4j
public class GovernanceConfigClient implements RemoteConfigClient {

    private final GovernanceChannelManager channelManager;
    private final GovernanceConfigProperties properties;
    private GovernanceServiceGrpc.GovernanceServiceBlockingStub blockingStub;
    private GovernanceServiceGrpc.GovernanceServiceStub asyncStub;

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicInteger reconnectAttempts = new AtomicInteger(0);

    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private StreamObserver<ConfigChangeNotification> subscriptionStream;

    @Getter
    private final String serviceName;
    @Getter
    private final String environment;

    public GovernanceConfigClient(GovernanceChannelManager channelManager, GovernanceConfigProperties properties) {
        this.channelManager = channelManager;
        this.properties = properties;
        this.serviceName = properties.getServiceName();
        this.environment = properties.getEnvironment();
        refreshStubs();
    }

    private void refreshStubs() {
        this.blockingStub = GovernanceServiceGrpc.newBlockingStub(channelManager.getChannel());
        this.asyncStub = GovernanceServiceGrpc.newStub(channelManager.getChannel());
    }

    /**
     * 启动客户端
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            connect();
            log.info("GovernanceConfigClient started, serviceName={}, environment={}", serviceName, environment);
        }
    }

    /**
     * 停止客户端
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            reconnectScheduler.shutdown();
            connected.set(false);
            log.info("GovernanceConfigClient stopped");
        }
    }

    // ==================== 连接管理 ====================

    private void connect() {
        if (!running.get()) {
            return;
        }

        try {
            refreshStubs();
            connected.set(true);
            reconnectAttempts.set(0);

            if (properties.isEnableSubscribe()) {
                subscribeConfig();
            }

            log.info("Connected to governance server for config client");
        } catch (Exception e) {
            log.error("Failed to connect to governance server", e);
            connected.set(false);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!running.get()) {
            return;
        }

        int attempts = reconnectAttempts.incrementAndGet();
        int maxAttempts = properties.getMaxReconnectAttempts();
        int delayMs = calculateReconnectDelay(attempts);

        if (maxAttempts > 0 && attempts > maxAttempts) {
            log.error("Max reconnect attempts ({}) reached, stopping reconnect", maxAttempts);
            return;
        }

        log.info("Scheduling reconnect attempt {} in {} ms", attempts, delayMs);
        reconnectScheduler.schedule(this::connect, delayMs, TimeUnit.MILLISECONDS);
    }

    private int calculateReconnectDelay(int attempts) {
        int baseDelay = properties.getReconnectIntervalMs();
        int maxDelay = properties.getMaxReconnectIntervalMs();
        // 指数退避：delay = baseDelay * 2^(attempts-1)，上限为 maxDelay
        int delay = baseDelay * (int) Math.pow(2, attempts - 1);
        return Math.min(delay, maxDelay);
    }

    // ==================== 配置获取 ====================

    @Override
    public Optional<String> getConfig(@NonNull String key) {
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }

        return fetchConfigWithRetry(key);
    }

    private Optional<String> fetchConfigWithRetry(String key) {
        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                GetConfigsRequest request = GetConfigsRequest.newBuilder()
                        .addKeys(key)
                        .build();

                GetConfigsResponse response = blockingStub.getConfigs(request);
                String value = response.getConfigsMap().get(key);
                if (value != null) {
                    configCache.put(key, value);
                    connected.set(true);
                }
                return Optional.ofNullable(value);
            } catch (Exception e) {
                log.warn("Failed to get config '{}' (attempt {}): {}", key, i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to get config after {} retries: {}", maxRetries + 1, key);
        return Optional.empty();
    }

    @Override
    public Optional<String> getConfig(@NonNull String group, @NonNull String key) {
        return getConfig(group + "." + key);
    }

    @Override
    public Map<String, String> getConfigs(@NonNull String prefix) {
        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                GetConfigsRequest request = GetConfigsRequest.newBuilder()
                        .setPrefix(prefix)
                        .build();

                GetConfigsResponse response = blockingStub.getConfigs(request);
                configCache.putAll(response.getConfigsMap());
                connected.set(true);
                return response.getConfigsMap();
            } catch (Exception e) {
                log.warn("Failed to get configs with prefix '{}' (attempt {}): {}", prefix, i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to get configs after {} retries: {}", maxRetries + 1, prefix);
        return Map.of();
    }

    public Map<String, String> getConfigs(List<String> keys) {
        GetConfigsRequest.Builder builder = GetConfigsRequest.newBuilder();
        keys.forEach(builder::addKeys);
        GetConfigsRequest request = builder.build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            configCache.putAll(response.getConfigsMap());
            connected.set(true);
            return response.getConfigsMap();
        } catch (Exception e) {
            log.error("Failed to get configs for keys: {}", keys, e);
            connected.set(false);
            return Map.of();
        }
    }

    // ==================== 配置发布 ====================

    @Override
    public boolean publishConfig(@NonNull String key, @NonNull String value) {
        return publishConfig(key, value, null, null, null, null, null, null, null, false);
    }

    @Override
    public boolean publishConfig(@NonNull String group, @NonNull String key, @NonNull String value) {
        return publishConfig(group + "." + key, value);
    }

    public boolean publishConfig(String key, String value, String reason, String operator,
                                  String serviceName, String environment, String displayName,
                                  String type, String defaultValue, boolean deprecated) {
        PublishConfigRequest.Builder builder = PublishConfigRequest.newBuilder()
                .setKey(key)
                .setValue(value != null ? value : "")
                .setReason(reason != null ? reason : "")
                .setOperator(operator != null ? operator : "");

        if (serviceName != null) {
            builder.setServiceName(serviceName);
        }
        if (environment != null) {
            builder.setEnvironment(environment);
        }
        if (displayName != null) {
            builder.setDisplayName(displayName);
        }
        if (type != null) {
            builder.setType(type);
        }
        if (defaultValue != null) {
            builder.setDefaultValue(defaultValue);
        }
        builder.setDeprecated(deprecated);

        PublishConfigRequest request = builder.build();

        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                PublishConfigResponse response = blockingStub.publishConfig(request);
                if (response.getSuccess()) {
                    if (value != null) {
                        configCache.put(key, value);
                    }
                    connected.set(true);
                    return true;
                } else {
                    log.error("Failed to publish config: {}, error: {}", key, response.getErrorMessage());
                    return false;
                }
            } catch (Exception e) {
                log.warn("Failed to publish config '{}' (attempt {}): {}", key, i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to publish config after {} retries: {}", maxRetries + 1, key);
        return false;
    }

    // ==================== 配置订阅 ====================

    private void subscribeConfig() {
        if (!running.get() || !connected.get()) {
            return;
        }

        SubscribeConfigRequest request = SubscribeConfigRequest.newBuilder()
                .setServiceName(serviceName != null ? serviceName : "")
                .build();

        subscriptionStream = new StreamObserver<>() {
            @Override
            public void onNext(ConfigChangeNotification notification) {
                log.info("Received config change: key={}, type={}",
                        notification.getKey(), notification.getChangeType());

                String key = notification.getKey();
                String oldValue = configCache.get(key);
                String newValue = notification.getValue();

                if (notification.getChangeType() == ChangeType.CHANGE_TYPE_DELETE) {
                    configCache.remove(key);
                    notifyListeners(key, oldValue, null);
                } else {
                    configCache.put(key, newValue);
                    notifyListeners(key, oldValue, newValue);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Config subscription error: {}", t.getMessage());
                connected.set(false);
                subscriptionStream = null;

                if (running.get()) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onCompleted() {
                log.info("Config subscription stream completed");
                connected.set(false);
                subscriptionStream = null;

                if (running.get()) {
                    scheduleReconnect();
                }
            }
        };

        try {
            asyncStub.subscribeConfig(request, subscriptionStream);
            log.info("Config subscription established");
        } catch (Exception e) {
            log.error("Failed to establish config subscription", e);
            connected.set(false);
            scheduleReconnect();
        }
    }

    // ==================== 监听器管理 ====================

    @Override
    public void addListener(@NonNull String key, @NonNull ConfigChangeListener listener) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("Added config listener for key: {}", key);
    }

    @Override
    public void addListener(@NonNull String group, @NonNull String key, @NonNull ConfigChangeListener listener) {
        addListener(group + "." + key, listener);
    }

    @Override
    public void removeListener(@NonNull String key) {
        listeners.remove(key);
        log.debug("Removed config listener for key: {}", key);
    }

    @Override
    public void removeListener(@NonNull String group, @NonNull String key) {
        removeListener(group + "." + key);
    }

    // ==================== 其他方法 ====================

    @Override
    public void refresh() {
        int maxRetries = properties.getRetryCount();
        for (int i = 0; i <= maxRetries; i++) {
            try {
                GetConfigsResponse response = blockingStub.getConfigs(
                        GetConfigsRequest.newBuilder().build()
                );
                configCache.clear();
                configCache.putAll(response.getConfigsMap());
                connected.set(true);
                log.info("Config cache refreshed, size: {}", configCache.size());
                return;
            } catch (Exception e) {
                log.warn("Failed to refresh config cache (attempt {}): {}", i + 1, e.getMessage());
                connected.set(false);

                if (i < maxRetries) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        refreshStubs();
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        log.error("Failed to refresh config cache after {} retries", maxRetries + 1);
    }

    @Override
    public String getClientName() {
        return "governance-config-client";
    }

    public Map<String, String> getConfigCache() {
        return Map.copyOf(configCache);
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isConnected() {
        return connected.get();
    }

    private void notifyListeners(String key, String oldValue, String newValue) {
        List<ConfigChangeListener> listenerList = listeners.get(key);
        if (listenerList != null) {
            io.github.afgprojects.framework.core.api.config.ConfigChangeEvent.ConfigChangeType changeType;
            if (oldValue == null && newValue != null) {
                changeType = io.github.afgprojects.framework.core.api.config.ConfigChangeEvent.ConfigChangeType.ADDED;
            } else if (oldValue != null && newValue == null) {
                changeType = io.github.afgprojects.framework.core.api.config.ConfigChangeEvent.ConfigChangeType.DELETED;
            } else {
                changeType = io.github.afgprojects.framework.core.api.config.ConfigChangeEvent.ConfigChangeType.MODIFIED;
            }

            io.github.afgprojects.framework.core.api.config.ConfigChangeEvent event =
                    new io.github.afgprojects.framework.core.api.config.ConfigChangeEvent(
                            key, "", oldValue, newValue, changeType
                    );
            for (ConfigChangeListener listener : listenerList) {
                try {
                    listener.onChange(event);
                } catch (Exception e) {
                    log.warn("Failed to notify listener for key: {}", key, e);
                }
            }
        }
    }
}