package io.github.afgprojects.framework.integration.config.apollo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Apollo 配置中心客户端实现
 *
 * <p>实现 {@link RemoteConfigClient} 接口，集成 Apollo 配置中心
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     apollo:
 *       enabled: true
 *       namespace: application
 * </pre>
 *
 * <h3>Apollo 系统属性</h3>
 * <ul>
 *   <li>app.id - 应用 ID</li>
 *   <li>apollo.meta - Apollo Meta Server 地址</li>
 *   <li>apollo.cluster - 集群名称</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class ApolloConfigClient implements RemoteConfigClient, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ApolloConfigClient.class);

    private final String defaultNamespace;
    private final Map<String, Config> apolloConfigs = new ConcurrentHashMap<>();
    private final Map<String, String> configCache = new ConcurrentHashMap<>();
    private final Map<String, com.ctrip.framework.apollo.ConfigChangeListener> apolloListeners = new ConcurrentHashMap<>();

    /**
     * 创建 Apollo 配置客户端
     *
     * @param defaultNamespace 默认命名空间
     */
    public ApolloConfigClient(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
        log.info("Apollo config client initialized, namespace: {}", defaultNamespace);
    }

    @Override
    public Optional<String> getConfig(@NonNull String key) {
        return getConfig(defaultNamespace, key);
    }

    @Override
    public Optional<String> getConfig(@NonNull String namespace, @NonNull String key) {
        try {
            Config config = getConfigInstance(namespace);
            String value = config.getProperty(key, null);
            if (value != null) {
                configCache.put(cacheKey(namespace, key), value);
            }
            return Optional.ofNullable(value);
        } catch (Exception e) {
            log.error("Failed to get config: namespace={}, key={}", namespace, key, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getConfigs(@NonNull String prefix) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configCache.entrySet()) {
            if (entry.getKey().endsWith(":" + prefix) || entry.getKey().contains(":" + prefix + ".")) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        // 同时从默认命名空间获取
        Config config = getConfigInstance(defaultNamespace);
        for (String key : config.getPropertyNames()) {
            if (key.startsWith(prefix)) {
                result.put(key, config.getProperty(key, ""));
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean publishConfig(@NonNull String key, @NonNull String value) {
        return publishConfig(defaultNamespace, key, value);
    }

    @Override
    public boolean publishConfig(@NonNull String namespace, @NonNull String key, @NonNull String value) {
        log.warn("Apollo does not support programmatic config publishing via client. Use Apollo Portal instead.");
        return false;
    }

    @Override
    public void addListener(@NonNull String key, @NonNull ConfigChangeListener listener) {
        addListener(defaultNamespace, key, listener);
    }

    @Override
    public void addListener(@NonNull String namespace, @NonNull String key, @NonNull ConfigChangeListener listener) {
        String listenerKey = cacheKey(namespace, key);

        if (apolloListeners.containsKey(listenerKey)) {
            log.debug("Apollo listener already registered for: {}", listenerKey);
            return;
        }

        Config config = getConfigInstance(namespace);

        com.ctrip.framework.apollo.ConfigChangeListener apolloListener = changeEvent -> {
            for (String changedKey : changeEvent.changedKeys()) {
                if (changedKey.equals(key)) {
                    log.info("Config changed: namespace={}, key={}", namespace, key);
                    try {
                        String oldValue = configCache.get(listenerKey);
                        String newValue = config.getProperty(key, null);

                        if (newValue != null) {
                            configCache.put(listenerKey, newValue);
                        }

                        ConfigChangeEvent.ConfigChangeType changeType = determineChangeType(oldValue, newValue);

                        ConfigChangeEvent event = new ConfigChangeEvent(
                                key, namespace, oldValue, newValue, changeType);

                        listener.onChange(event);
                    } catch (Exception e) {
                        log.error("Failed to process config change: namespace={}, key={}", namespace, key, e);
                    }
                }
            }
        };

        config.addChangeListener(apolloListener);
        apolloListeners.put(listenerKey, apolloListener);
        log.info("Added config listener for namespace: {}, key: {}", namespace, key);
    }

    @Override
    public void removeListener(@NonNull String key) {
        removeListener(defaultNamespace, key);
    }

    @Override
    public void removeListener(@NonNull String namespace, @NonNull String key) {
        String listenerKey = cacheKey(namespace, key);
        com.ctrip.framework.apollo.ConfigChangeListener apolloListener = apolloListeners.remove(listenerKey);
        if (apolloListener != null) {
            Config config = apolloConfigs.get(namespace);
            if (config != null) {
                config.removeChangeListener(apolloListener);
            }
        }
        log.info("Removed config listener for namespace: {}, key: {}", namespace, key);
    }

    @Override
    public void refresh() {
        log.debug("Refreshing Apollo config cache");
        configCache.clear();
    }

    @Override
    public String getClientName() {
        return "apollo";
    }

    /**
     * 健康检查
     *
     * @return 如果连接正常返回 true
     */
    public boolean isHealthy() {
        try {
            Config config = ConfigService.getConfig(defaultNamespace);
            return config != null;
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
        for (Map.Entry<String, com.ctrip.framework.apollo.ConfigChangeListener> entry : apolloListeners.entrySet()) {
            String[] parts = entry.getKey().split(":", 2);
            if (parts.length == 2) {
                Config config = apolloConfigs.get(parts[0]);
                if (config != null) {
                    config.removeChangeListener(entry.getValue());
                }
            }
        }
        apolloListeners.clear();
        configCache.clear();
        log.info("Apollo config client closed");
    }

    private Config getConfigInstance(String namespace) {
        return apolloConfigs.computeIfAbsent(namespace, ns -> {
            ConfigFileFormat format = ConfigFileFormat.Properties;
            String namespaceWithFormat = ns + "." + format.getValue();
            return ConfigService.getConfig(namespaceWithFormat);
        });
    }

    private String cacheKey(String namespace, String key) {
        return namespace + ":" + key;
    }

    private ConfigChangeEvent.ConfigChangeType determineChangeType(@Nullable String oldValue, @Nullable String newValue) {
        if (oldValue == null) {
            return ConfigChangeEvent.ConfigChangeType.ADDED;
        } else if (newValue == null) {
            return ConfigChangeEvent.ConfigChangeType.DELETED;
        } else {
            return ConfigChangeEvent.ConfigChangeType.MODIFIED;
        }
    }
}
