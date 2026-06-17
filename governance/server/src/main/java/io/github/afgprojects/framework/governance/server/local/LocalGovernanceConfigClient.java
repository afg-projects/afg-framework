package io.github.afgprojects.framework.governance.server.local;

import io.github.afgprojects.framework.core.api.config.ConfigChangeEvent;
import io.github.afgprojects.framework.core.api.config.ConfigChangeListener;
import io.github.afgprojects.framework.core.api.config.RemoteConfigClient;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigGroup;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigItem;
import io.github.afgprojects.framework.governance.server.entity.config.ConfigValue;
import io.github.afgprojects.framework.governance.server.service.config.ConfigGroupService;
import io.github.afgprojects.framework.governance.server.service.config.ConfigItemService;
import io.github.afgprojects.framework.governance.server.service.config.ConfigValueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.event.EventListener;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本地模式配置客户端
 * <p>
 * 当 governance-server 和 governance-client 在同一个 JVM 时，
 * 通过 Spring Bean 直接调用 server 端的 Service 层替代 gRPC 通信。
 * <p>
 * 实现 {@link RemoteConfigClient} 接口，注册为 Bean 后会阻止
 * {@code GovernanceClientAutoConfiguration} 创建 gRPC 客户端
 * （因为其使用了 {@code @ConditionalOnMissingBean(RemoteConfigClient.class)}）。
 *
 * @author afg-projects
 */
@Slf4j
@RequiredArgsConstructor
public class LocalGovernanceConfigClient implements RemoteConfigClient {

    private final ConfigValueService configValueService;
    private final ConfigItemService configItemService;
    private final ConfigGroupService configGroupService;

    /**
     * 本地配置缓存
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * 配置变更监听器：key -> listeners
     */
    private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();

    // ==================== 配置获取 ====================

    @Override
    public Optional<String> getConfig(@NonNull String key) {
        String cachedValue = configCache.get(key);
        if (cachedValue != null) {
            return Optional.of(cachedValue);
        }

        Optional<String> value = configValueService.getValueByCode(key);
        value.ifPresent(v -> configCache.put(key, v));
        return value;
    }

    @Override
    public Optional<String> getConfig(@NonNull String group, @NonNull String key) {
        return getConfig(group + "." + key);
    }

    @Override
    public Map<String, String> getConfigs(@NonNull String prefix) {
        if (prefix.isEmpty()) {
            Map<String, String> allValues = configValueService.getAllValues();
            configCache.clear();
            configCache.putAll(allValues);
            return allValues;
        }

        Map<String, String> values = configValueService.getValuesByPrefix(prefix);
        configCache.putAll(values);
        return values;
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

    /**
     * 带元数据的配置发布，支持 ConfigAutoRegistrar 自动注册场景。
     * <p>
     * 如果 ConfigItem 不存在则自动创建（参考 GovernanceGrpcServiceImpl 的逻辑），
     * 包括自动创建对应的 ConfigGroup。
     *
     * @param key          配置键
     * @param value        配置值
     * @param reason       变更原因
     * @param operator     操作人
     * @param serviceName  服务名称（用于自动创建分组）
     * @param environment  环境（用于自动创建分组）
     * @param displayName  显示名称
     * @param type         配置类型（Java 全限定类名）
     * @param defaultValue 默认值
     * @param deprecated   是否已废弃
     * @return 是否发布成功
     */
    public boolean publishConfig(String key, String value, String reason, String operator,
                                 String serviceName, String environment, String displayName,
                                 String type, String defaultValue, boolean deprecated) {
        try {
            ConfigItem item = resolveOrCreateItem(key, serviceName, environment, displayName, type, defaultValue, deprecated);

            // 废弃的配置项不更新值
            if (item.getIsDeprecated() != null && item.getIsDeprecated()) {
                log.debug("Skipping value update for deprecated config: {}", key);
                return true;
            }

            String effectiveValue = resolveEffectiveValue(value, defaultValue, key);
            updateConfigValueIfNeeded(item, effectiveValue, reason, operator, key);

            return true;
        } catch (Exception e) {
            log.error("Failed to publish config: {}", key, e);
            return false;
        }
    }

    /**
     * 查找或自动创建 ConfigItem，如果已存在则更新元数据。
     */
    private ConfigItem resolveOrCreateItem(String key, String serviceName, String environment,
                                           String displayName, String type, String defaultValue,
                                           boolean deprecated) {
        ConfigItem item = configItemService.findByCode(key).orElse(null);

        if (item == null) {
            item = createConfigItem(key, serviceName, environment, displayName, type, defaultValue, deprecated);
            log.info("Auto-created config item: {} in group {} (type={}, default={})",
                    key, (serviceName != null ? serviceName : "default") + "-" +
                    (environment != null ? environment : "dev"), type, defaultValue);
        } else {
            updateItemMetadataIfNeeded(item, displayName, type, defaultValue, deprecated, key);
        }

        return item;
    }

    /**
     * 如果提供了新的元数据，更新 ConfigItem 的元数据字段。
     */
    private void updateItemMetadataIfNeeded(ConfigItem item, String displayName, String type,
                                            String defaultValue, boolean deprecated, String key) {
        boolean needsUpdate = false;
        if (displayName != null && !displayName.isEmpty() && !displayName.equals(item.getName())) {
            item.setName(displayName);
            needsUpdate = true;
        }
        if (type != null && !type.isEmpty() && !type.equals(item.getType())) {
            item.setType(mapTypeToSimple(type));
            needsUpdate = true;
        }
        if (defaultValue != null && !defaultValue.isEmpty() && !defaultValue.equals(item.getDefaultValue())) {
            item.setDefaultValue(defaultValue);
            needsUpdate = true;
        }
        if (deprecated && (item.getIsDeprecated() == null || !item.getIsDeprecated())) {
            item.setIsDeprecated(true);
            needsUpdate = true;
        }
        if (needsUpdate) {
            item.setUpdatedAt(Instant.now());
            configItemService.update(item.getId(), item);
            log.debug("Updated config item metadata: {}", key);
        }
    }

    /**
     * 解析有效的配置值：如果值为空且有默认值，使用默认值。
     */
    private String resolveEffectiveValue(String value, String defaultValue, String key) {
        if (value != null && !value.isEmpty()) {
            return value;
        }
        if (defaultValue != null && !defaultValue.isEmpty()) {
            log.debug("Using default value for {}: {}", key, defaultValue);
            return defaultValue;
        }
        return value;
    }

    /**
     * 根据有效值决定是否更新配置值。
     */
    private void updateConfigValueIfNeeded(ConfigItem item, String effectiveValue,
                                           String reason, String operator, String key) {
        String reasonArg = reason != null ? reason : "";
        String operatorArg = operator != null ? operator : "";

        if (effectiveValue != null && !effectiveValue.isEmpty()) {
            log.info("Updating config value for {}: {}", key, effectiveValue);
            configValueService.updateValue(item.getId(), effectiveValue, reasonArg, operatorArg);
            configCache.put(key, effectiveValue);
            return;
        }

        // 值为空，检查是否已有值
        String existingValue = configValueService.getValueByCode(key).orElse(null);
        if (existingValue == null) {
            log.debug("Creating empty config value for: {}", key);
            configValueService.updateValue(item.getId(), "", reasonArg, operatorArg);
        } else {
            log.debug("Skipping config value update for {}: value is empty and existing value exists", key);
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
        Map<String, String> allValues = configValueService.getAllValues();
        configCache.clear();
        configCache.putAll(allValues);
        log.info("Config cache refreshed, size: {}", configCache.size());
    }

    @Override
    public String getClientName() {
        return "governance-config-local";
    }

    // ==================== 配置变更监听 ====================

    /**
     * 监听 ConfigValueService 发布的配置变更事件，更新缓存并通知 listeners。
     *
     * @param event 配置变更事件
     */
    @EventListener
    public void onConfigChanged(ConfigValueService.ConfigChangedEvent event) {
        String key = event.key();
        String newValue = event.value();
        String oldValue = configCache.get(key);

        // 更新缓存
        if (newValue != null) {
            configCache.put(key, newValue);
        } else {
            configCache.remove(key);
        }

        // 通知监听器
        notifyListeners(key, oldValue, newValue);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 自动创建 ConfigItem，包括自动创建对应的 ConfigGroup。
     * 逻辑参考 GovernanceGrpcServiceImpl.createConfigItem。
     */
    private ConfigItem createConfigItem(String code, String serviceName, String environment,
                                        String displayName, String metaType, String metaDefaultValue,
                                        boolean metaDeprecated) {
        Instant now = Instant.now();

        // 获取或创建分组
        String groupCode = (serviceName == null || serviceName.isEmpty() ? "default" : serviceName) + "-" +
                (environment == null || environment.isEmpty() ? "dev" : environment);
        ConfigGroup group = getOrCreateGroup(groupCode, serviceName, environment);

        ConfigItem item = new ConfigItem();
        item.setCode(code);
        item.setName(displayName == null || displayName.isEmpty() ? code : displayName);
        item.setDescription("Auto-created from client registration");
        item.setType(metaType == null || metaType.isEmpty() ? "STRING" : mapTypeToSimple(metaType));
        item.setDefaultValue(metaDefaultValue == null || metaDefaultValue.isEmpty() ? null : metaDefaultValue);
        item.setIsDeprecated(metaDeprecated);
        item.setGroupId(group.getId());
        item.setCreatedAt(now);
        item.setUpdatedAt(now);

        return configItemService.create(item);
    }

    /**
     * 获取或创建配置组（按服务名+环境）。
     * 逻辑参考 GovernanceGrpcServiceImpl.getOrCreateGroup。
     */
    private ConfigGroup getOrCreateGroup(String groupCode, String serviceName, String environment) {
        return configGroupService.findByCode(groupCode)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    ConfigGroup group = new ConfigGroup();
                    group.setCode(groupCode);
                    // 中文分组名：服务名-环境
                    String envName = switch (environment != null ? environment : "dev") {
                        case "dev" -> "开发环境";
                        case "test" -> "测试环境";
                        case "prod" -> "生产环境";
                        default -> (environment == null || environment.isEmpty()) ? "开发环境" : environment;
                    };
                    group.setName((serviceName == null || serviceName.isEmpty() ? "默认服务" : serviceName) + "-" + envName);
                    group.setDescription("自动创建的配置分组");
                    group.setSort(0);
                    group.setStatus(1);
                    group.setCreatedAt(now);
                    group.setUpdatedAt(now);
                    return configGroupService.create(group);
                });
    }

    /**
     * 将 Java 类型映射为简单类型名称。
     * 逻辑参考 GovernanceGrpcServiceImpl.mapTypeToSimple。
     */
    private String mapTypeToSimple(String javaType) {
        if (javaType == null || javaType.isEmpty()) {
            return "STRING";
        }
        if (javaType.equals("java.lang.Boolean") || javaType.equals("boolean")) {
            return "BOOLEAN";
        }
        if (javaType.equals("java.lang.Integer") || javaType.equals("int")) {
            return "INTEGER";
        }
        if (javaType.equals("java.lang.Long") || javaType.equals("long")) {
            return "LONG";
        }
        if (javaType.equals("java.lang.Double") || javaType.equals("double") ||
                javaType.equals("java.lang.Float") || javaType.equals("float")) {
            return "DOUBLE";
        }
        if (javaType.equals("java.time.Duration")) {
            return "DURATION";
        }
        if (javaType.startsWith("java.util.List")) {
            return "LIST";
        }
        if (javaType.startsWith("java.util.Map")) {
            return "MAP";
        }
        return "STRING";
    }

    /**
     * 通知配置变更监听器
     */
    private void notifyListeners(String key, String oldValue, String newValue) {
        List<ConfigChangeListener> listenerList = listeners.get(key);
        if (listenerList != null) {
            ConfigChangeEvent.ConfigChangeType changeType;
            if (oldValue == null && newValue != null) {
                changeType = ConfigChangeEvent.ConfigChangeType.ADDED;
            } else if (oldValue != null && newValue == null) {
                changeType = ConfigChangeEvent.ConfigChangeType.DELETED;
            } else {
                changeType = ConfigChangeEvent.ConfigChangeType.MODIFIED;
            }

            ConfigChangeEvent event = new ConfigChangeEvent(key, "", oldValue, newValue, changeType);
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
