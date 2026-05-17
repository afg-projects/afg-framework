package io.github.afgprojects.framework.integration.governance.client;

import io.github.afgprojects.framework.integration.governance.api.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Governance gRPC 客户端
 * <p>
 * 提供配置获取、发布和订阅功能
 *
 * @author afg-projects
 */
@Slf4j
public class GovernanceClient {

    private final GovernanceClientProperties properties;
    private final ManagedChannel channel;
    private final GovernanceServiceGrpc.GovernanceServiceBlockingStub blockingStub;
    private final GovernanceServiceGrpc.GovernanceServiceStub asyncStub;

    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private volatile boolean running = false;

    public GovernanceClient(GovernanceClientProperties properties) {
        this.properties = properties;
        this.channel = ManagedChannelBuilder.forTarget(properties.getServerAddr())
            .usePlaintext()
            .build();
        this.blockingStub = GovernanceServiceGrpc.newBlockingStub(channel);
        this.asyncStub = GovernanceServiceGrpc.newStub(channel);
    }

    /**
     * 启动客户端
     */
    public void start() {
        if (running) {
            return;
        }
        running = true;

        if (properties.isEnableConfigSubscribe()) {
            subscribeConfig();
        }

        log.info("GovernanceClient started, connected to {}", properties.getServerAddr());
    }

    /**
     * 停止客户端
     */
    public void stop() {
        running = false;
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("GovernanceClient stopped");
    }

    /**
     * 获取单个配置
     *
     * @param key 配置键
     * @return 配置值
     */
    public Optional<String> getConfig(String key) {
        // 先查缓存
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }

        // 从服务端获取
        GetConfigsRequest request = GetConfigsRequest.newBuilder()
            .addKeys(key)
            .build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            String value = response.getConfigsMap().get(key);
            if (value != null) {
                configCache.put(key, value);
            }
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Failed to get config: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * 批量获取配置
     *
     * @param prefix 配置前缀
     * @return 配置键值对
     */
    public Map<String, String> getConfigs(String prefix) {
        GetConfigsRequest request = GetConfigsRequest.newBuilder()
            .setPrefix(prefix)
            .build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            configCache.putAll(response.getConfigsMap());
            return response.getConfigsMap();
        } catch (Exception e) {
            log.error("Failed to get configs with prefix: {}", prefix, e);
            return Map.of();
        }
    }

    /**
     * 批量获取指定键的配置
     *
     * @param keys 配置键列表
     * @return 配置键值对
     */
    public Map<String, String> getConfigs(java.util.List<String> keys) {
        GetConfigsRequest.Builder builder = GetConfigsRequest.newBuilder();
        keys.forEach(builder::addKeys);
        GetConfigsRequest request = builder.build();

        try {
            GetConfigsResponse response = blockingStub.getConfigs(request);
            configCache.putAll(response.getConfigsMap());
            return response.getConfigsMap();
        } catch (Exception e) {
            log.error("Failed to get configs for keys: {}", keys, e);
            return Map.of();
        }
    }

    /**
     * 发布配置
     *
     * @param key      配置键
     * @param value    配置值
     * @param reason   变更原因
     * @param operator 操作人
     * @return 是否成功
     */
    public boolean publishConfig(String key, String value, String reason, String operator) {
        PublishConfigRequest request = PublishConfigRequest.newBuilder()
            .setKey(key)
            .setValue(value)
            .setReason(reason != null ? reason : "")
            .setOperator(operator != null ? operator : "")
            .build();

        try {
            PublishConfigResponse response = blockingStub.publishConfig(request);
            if (response.getSuccess()) {
                configCache.put(key, value);
                return true;
            } else {
                log.error("Failed to publish config: {}, error: {}", key, response.getErrorMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to publish config: {}", key, e);
            return false;
        }
    }

    /**
     * 订阅配置变更
     */
    private void subscribeConfig() {
        SubscribeConfigRequest request = SubscribeConfigRequest.newBuilder()
            .setServiceName(properties.getServiceName() != null ? properties.getServiceName() : "")
            .build();

        asyncStub.subscribeConfig(request, new StreamObserver<>() {
            @Override
            public void onNext(ConfigChangeNotification notification) {
                log.info("Received config change: key={}, type={}",
                    notification.getKey(), notification.getChangeType());

                // 更新缓存
                if (notification.getChangeType() == ChangeType.CHANGE_TYPE_DELETE) {
                    configCache.remove(notification.getKey());
                } else {
                    configCache.put(notification.getKey(), notification.getValue());
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Config subscription error", t);
                // 重连逻辑
                if (running) {
                    try {
                        Thread.sleep(properties.getRetryIntervalMs());
                        subscribeConfig();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }

            @Override
            public void onCompleted() {
                log.info("Config subscription completed");
            }
        });
    }

    /**
     * 获取配置缓存
     *
     * @return 配置缓存快照
     */
    public Map<String, String> getConfigCache() {
        return Map.copyOf(configCache);
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        try {
            GetConfigsResponse response = blockingStub.getConfigs(
                GetConfigsRequest.newBuilder().build()
            );
            configCache.clear();
            configCache.putAll(response.getConfigsMap());
            log.info("Config cache refreshed, size: {}", configCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh config cache", e);
        }
    }

    /**
     * 获取客户端状态
     *
     * @return 是否运行中
     */
    public boolean isRunning() {
        return running && !channel.isShutdown();
    }
}
