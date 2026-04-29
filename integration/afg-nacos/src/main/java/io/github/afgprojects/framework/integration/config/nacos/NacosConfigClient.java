package io.github.afgprojects.framework.integration.config.nacos;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;

/**
 * Nacos 配置中心客户端实现
 *
 * <p>实现 {@link RemoteConfigClient} 接口，集成 Nacos 配置中心
 *
 * <h3>配置示例</h3>
 * <pre>
 * afg:
 *   config:
 *     nacos:
 *       enabled: true
 *       server-addr: ${NACOS_ADDR:localhost:8848}
 *       namespace: ${NACOS_NAMESPACE:}
 *       group: DEFAULT_GROUP
 *       username: ${NACOS_USERNAME:}
 *       password: ${NACOS_PASSWORD:}
 * </pre>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class NacosConfigClient implements RemoteConfigClient, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigClient.class);

    private final ConfigService configService;
    private final String defaultGroup;
    private final Map<String, Listener> nacosListeners = new ConcurrentHashMap<>();
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * 创建 Nacos 配置客户端
     *
     * @param configService Nacos ConfigService 实例
     * @param defaultGroup  默认配置分组
     */
    public NacosConfigClient(ConfigService configService, String defaultGroup) {
        this.configService = configService;
        this.defaultGroup = defaultGroup;
        log.info("Nacos config client initialized, group: {}", defaultGroup);
    }

    /**
     * 使用配置属性创建 Nacos 配置客户端
     *
     * @param properties Nacos 配置属性
     * @throws NacosException 如果连接失败
     */
    public NacosConfigClient(NacosConfigProperties properties) throws NacosException {
        this.configService = createConfigService(properties);
        this.defaultGroup = properties.getGroup();
        log.info("Nacos config client initialized, server: {}, group: {}",
                properties.getServerAddr(), properties.getGroup());
    }

    private ConfigService createConfigService(NacosConfigProperties props) throws NacosException {
        Properties nacosProps = new Properties();
        nacosProps.setProperty("serverAddr", props.getServerAddr());

        if (props.getNamespace() != null && !props.getNamespace().isEmpty()) {
            nacosProps.setProperty("namespace", props.getNamespace());
        }

        if (props.getUsername() != null && props.getPassword() != null) {
            nacosProps.setProperty("username", props.getUsername());
            nacosProps.setProperty("password", props.getPassword());
        }

        if (props.getAccessToken() != null && !props.getAccessToken().isEmpty()) {
            nacosProps.setProperty("accessToken", props.getAccessToken());
        }

        if (props.getConnectTimeout() > 0) {
            nacosProps.setProperty("configLongPollTimeout", String.valueOf(props.getConnectTimeout()));
        }

        if (props.getReadTimeout() > 0) {
            nacosProps.setProperty("configReadTimeout", String.valueOf(props.getReadTimeout()));
        }

        return NacosFactory.createConfigService(nacosProps);
    }

    @Override
    public Optional<String> getConfig(@NonNull String key) {
        return getConfig(defaultGroup, key);
    }

    @Override
    public Optional<String> getConfig(@NonNull String group, @NonNull String key) {
        try {
            String config = configService.getConfig(key, group, 5000);
            if (config != null) {
                configCache.put(cacheKey(group, key), config);
            }
            return Optional.ofNullable(config);
        } catch (NacosException e) {
            log.error("Failed to get config: group={}, key={}", group, key, e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, String> getConfigs(@NonNull String prefix) {
        // Nacos 不直接支持前缀查询，返回缓存中匹配的配置
        // 实际应用中建议使用完整的 dataId 或通过 Nacos Open API 实现
        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> entry : configCache.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public boolean publishConfig(@NonNull String key, @NonNull String value) {
        return publishConfig(defaultGroup, key, value);
    }

    @Override
    public boolean publishConfig(@NonNull String group, @NonNull String key, @NonNull String value) {
        try {
            boolean success = configService.publishConfig(key, group, value);
            if (success) {
                log.info("Published config: group={}, key={}", group, key);
                configCache.put(cacheKey(group, key), value);
            }
            return success;
        } catch (NacosException e) {
            log.error("Failed to publish config: group={}, key={}", group, key, e);
            return false;
        }
    }

    @Override
    public void addListener(@NonNull String key, @NonNull ConfigChangeListener listener) {
        addListener(defaultGroup, key, listener);
    }

    @Override
    public void addListener(@NonNull String group, @NonNull String key, @NonNull ConfigChangeListener listener) {
        String listenerKey = cacheKey(group, key);

        // 如果已经注册过 Nacos 监听器，不再重复注册
        if (nacosListeners.containsKey(listenerKey)) {
            log.debug("Listener already registered for: {}", listenerKey);
            return;
        }

        Listener nacosListener = new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
                log.info("Config changed: group={}, key={}", group, key);
                try {
                    String oldValue = configCache.get(listenerKey);
                    String newValue = configInfo;

                    configCache.put(listenerKey, newValue);

                    ConfigChangeEvent.ConfigChangeType changeType;
                    if (oldValue == null) {
                        changeType = ConfigChangeEvent.ConfigChangeType.ADDED;
                    } else if (newValue == null || newValue.isEmpty()) {
                        changeType = ConfigChangeEvent.ConfigChangeType.DELETED;
                    } else {
                        changeType = ConfigChangeEvent.ConfigChangeType.MODIFIED;
                    }

                    ConfigChangeEvent event = new ConfigChangeEvent(
                            key, group, oldValue, newValue, changeType);

                    listener.onChange(event);
                } catch (Exception e) {
                    log.error("Failed to process config change: group={}, key={}", group, key, e);
                }
            }

            @Override
            public Executor getExecutor() {
                return null;
            }
        };

        try {
            configService.addListener(key, group, nacosListener);
            nacosListeners.put(listenerKey, nacosListener);
            log.info("Added config listener: group={}, key={}", group, key);
        } catch (NacosException e) {
            log.error("Failed to add listener: group={}, key={}", group, key, e);
        }
    }

    @Override
    public void removeListener(@NonNull String key) {
        removeListener(defaultGroup, key);
    }

    @Override
    public void removeListener(@NonNull String group, @NonNull String key) {
        String listenerKey = cacheKey(group, key);
        Listener listener = nacosListeners.remove(listenerKey);
        if (listener != null) {
            configService.removeListener(key, group, listener);
            log.info("Removed config listener: group={}, key={}", group, key);
        }
    }

    @Override
    public void refresh() {
        // Nacos 客户端会自动刷新配置，这里刷新缓存
        log.debug("Refreshing Nacos config cache");
        // 清空缓存，下次获取时会重新从 Nacos 拉取
        configCache.clear();
    }

    @Override
    public String getClientName() {
        return "nacos";
    }

    /**
     * 健康检查
     *
     * @return 如果连接正常返回 true
     */
    public boolean isHealthy() {
        try {
            String status = configService.getServerStatus();
            return "UP".equals(status);
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
        try {
            // 移除所有监听器
            for (Map.Entry<String, Listener> entry : nacosListeners.entrySet()) {
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    configService.removeListener(parts[1], parts[0], entry.getValue());
                }
            }
            nacosListeners.clear();
            configCache.clear();
            log.info("Nacos config client closed");
        } catch (Exception e) {
            log.error("Failed to close nacos config client", e);
        }
    }

    private String cacheKey(String group, String key) {
        return group + ":" + key;
    }
}
