package io.github.afgprojects.framework.integration.config.consul;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.kv.model.GetValue;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Consul 配置中心客户端实现
 *
 * <p>实现 {@link RemoteConfigClient} 接口，集成 Consul KV Store
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     consul:
 *       enabled: true
 *       host: localhost
 *       port: 8500
 *       prefix: config/afg
 *       token: ${CONSUL_TOKEN:}
 * </pre>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class ConsulConfigClient implements RemoteConfigClient, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ConsulConfigClient.class);

    private final ConsulClient consulClient;
    private final String prefix;
    private final @Nullable String token;
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ScheduledFuture<?>> listenerFutures = new ConcurrentHashMap<>();

    /**
     * 创建 Consul 配置客户端
     *
     * @param host   Consul 主机地址
     * @param port   Consul 端口
     * @param prefix 配置前缀
     * @param token  ACL token（可选）
     */
    public ConsulConfigClient(String host, int port, String prefix, @Nullable String token) {
        this.consulClient = new ConsulClient(host, port);
        this.prefix = prefix;
        this.token = token;
        log.info("Consul config client initialized, host: {}, port: {}, prefix: {}", host, port, prefix);
    }

    /**
     * 使用配置属性创建 Consul 配置客户端
     *
     * @param properties Consul 配置属性
     */
    public ConsulConfigClient(ConsulConfigProperties properties) {
        this.consulClient = new ConsulClient(properties.getHost(), properties.getPort());
        this.prefix = properties.getPrefix();
        this.token = properties.getToken();
        log.info("Consul config client initialized, host: {}, port: {}, prefix: {}",
                properties.getHost(), properties.getPort(), properties.getPrefix());
    }

    @Override
    public Optional<String> getConfig(@NonNull String key) {
        return getConfig(prefix, key);
    }

    @Override
    public Optional<String> getConfig(@NonNull String group, @NonNull String key) {
        try {
            String fullPath = buildKey(group, key);
            GetValue value = consulClient.getKVValue(fullPath, token).getValue();

            if (value != null && value.getValue() != null) {
                String content = value.getDecodedValue();
                configCache.put(cacheKey(group, key), content);
                return Optional.of(content);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to get config: group={}, key={}", group, key, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getConfigs(@NonNull String prefixKey) {
        Map<String, String> result = new LinkedHashMap<>();
        try {
            String rootKey = prefix + "/" + prefixKey;
            var response = consulClient.getKVValues(rootKey, token);

            if (response.getValue() != null) {
                for (GetValue value : response.getValue()) {
                    String key = extractKey(value.getKey());
                    String content = value.getDecodedValue();
                    result.put(key, content);
                    configCache.put(cacheKey(prefix, key), content);
                }
            }
        } catch (Exception e) {
            log.error("Failed to get configs with prefix: {}", prefixKey, e);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean publishConfig(@NonNull String key, @NonNull String value) {
        return publishConfig(prefix, key, value);
    }

    @Override
    public boolean publishConfig(@NonNull String group, @NonNull String key, @NonNull String value) {
        try {
            String fullPath = buildKey(group, key);
            consulClient.setKVValue(fullPath, value, token, null);
            log.info("Published config: {}", fullPath);
            configCache.put(cacheKey(group, key), value);
            return true;
        } catch (Exception e) {
            log.error("Failed to publish config: group={}, key={}", group, key, e);
            return false;
        }
    }

    @Override
    public void addListener(@NonNull String key, @NonNull ConfigChangeListener listener) {
        addListener(prefix, key, listener);
    }

    @Override
    public void addListener(@NonNull String group, @NonNull String key, @NonNull ConfigChangeListener listener) {
        String listenerKey = cacheKey(group, key);
        log.info("Added config listener for: {}", listenerKey);

        // Consul 使用定时轮询来监听变化
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                String oldValue = configCache.get(listenerKey);
                getConfig(group, key).ifPresent(newValue -> {
                    if (!newValue.equals(oldValue)) {
                        log.info("Config changed: {}", listenerKey);
                        ConfigChangeEvent.ConfigChangeType changeType;
                        if (oldValue == null) {
                            changeType = ConfigChangeEvent.ConfigChangeType.ADDED;
                        } else {
                            changeType = ConfigChangeEvent.ConfigChangeType.MODIFIED;
                        }

                        ConfigChangeEvent event = new ConfigChangeEvent(
                                key, group, oldValue, newValue, changeType);

                        listener.onChange(event);
                    }
                });
            } catch (Exception e) {
                log.error("Polling error for: {}", listenerKey, e);
            }
        }, 5, 5, TimeUnit.SECONDS);

        listenerFutures.put(listenerKey, future);
    }

    @Override
    public void removeListener(@NonNull String key) {
        removeListener(prefix, key);
    }

    @Override
    public void removeListener(@NonNull String group, @NonNull String key) {
        String listenerKey = cacheKey(group, key);
        ScheduledFuture<?> future = listenerFutures.remove(listenerKey);
        if (future != null) {
            future.cancel(false);
            log.info("Removed config listener for: group={}, key={}", group, key);
        } else {
            log.warn("No listener found for: group={}, key={}", group, key);
        }
    }

    @Override
    public void refresh() {
        log.debug("Refreshing Consul config cache");
        configCache.clear();
    }

    @Override
    public String getClientName() {
        return "consul";
    }

    /**
     * 健康检查
     *
     * @return 如果连接正常返回 true
     */
    public boolean isHealthy() {
        try {
            var leader = consulClient.getStatusLeader();
            return leader.getValue() != null;
        } catch (Exception e) {
            log.error("Health check failed", e);
            return false;
        }
    }

    /**
     * 关闭客户端，释放资源
     */
    @Override
    public void close() {
        // 取消所有监听器任务
        listenerFutures.forEach((key, future) -> future.cancel(false));
        listenerFutures.clear();
        scheduler.shutdown();
        configCache.clear();
        log.info("Consul config client closed");
    }

    private String buildKey(String group, String key) {
        return group + "/" + key;
    }

    private String extractKey(String fullPath) {
        if (fullPath.startsWith(prefix + "/")) {
            return fullPath.substring(prefix.length() + 1);
        }
        return fullPath;
    }

    private String cacheKey(String group, String key) {
        return group + ":" + key;
    }
}