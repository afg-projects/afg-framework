package io.github.afgprojects.framework.core.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import io.github.afgprojects.framework.core.config.ConfigChangeEvent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.api.scheduler.DynamicTaskConfigSource;
import io.github.afgprojects.framework.core.api.scheduler.TaskDefinition;
import io.github.afgprojects.framework.core.config.AfgConfigRegistry;

/**
 * 配置中心动态任务配置源
 *
 * <p>基于 AfgConfigRegistry 实现的动态任务配置源
 *
 * <h3>配置格式</h3>
 * <pre>
 * afg:
 *   tasks:
 *     my-task:
 *       group: default
 *       cron: "0 0/5 * * * ?"
 *       enabled: true
 *       timeout: 30000
 *       max-retries: 3
 *       retry-delay: 1000
 *       description: "我的定时任务"
 * </pre>
 *
 * @since 1.0.0
 */
public class ConfigCenterTaskConfigSource implements DynamicTaskConfigSource {

    private static final Logger log = LoggerFactory.getLogger(ConfigCenterTaskConfigSource.class);

    private static final String CONFIG_PREFIX = "afg.tasks";

    private final AfgConfigRegistry configRegistry;
    private final CopyOnWriteArrayList<DynamicTaskConfigSource.ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final String configPrefix;

    /**
     * 创建配置中心任务配置源
     *
     * @param configRegistry 配置注册中心
     */
    public ConfigCenterTaskConfigSource(@NonNull AfgConfigRegistry configRegistry) {
        this(configRegistry, CONFIG_PREFIX);
    }

    /**
     * 创建配置中心任务配置源
     *
     * @param configRegistry 配置注册中心
     * @param configPrefix   配置前缀
     */
    public ConfigCenterTaskConfigSource(@NonNull AfgConfigRegistry configRegistry, @NonNull String configPrefix) {
        this.configRegistry = configRegistry;
        this.configPrefix = configPrefix;

        // 注册配置变更监听
        configRegistry.addListener(configPrefix, event -> {
            handleConfigChange(event);
        });

        log.info("ConfigCenterTaskConfigSource initialized with prefix: {}", configPrefix);
    }

    @Override
    @NonNull
    public List<TaskDefinition> loadAll() {
        List<TaskDefinition> definitions = new ArrayList<>();

        Map<String, Object> allConfigs = configRegistry.getAllConfigs();
        for (Map.Entry<String, Object> entry : allConfigs.entrySet()) {
            String taskId = extractTaskId(entry.getKey());
            if (taskId != null && entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                TaskDefinition definition = parseTaskDefinition(taskId, (Map<String, Object>) entry.getValue());
                if (definition != null) {
                    definitions.add(definition);
                }
            }
        }

        log.debug("Loaded {} task definitions from config center", definitions.size());
        return definitions;
    }

    @Override
    @NonNull
    public Optional<TaskDefinition> load(@NonNull String taskId) {
        String key = configPrefix + "." + taskId;
        Object value = configRegistry.getConfig(key);

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            TaskDefinition definition = parseTaskDefinition(taskId, (Map<String, Object>) value);
            return Optional.ofNullable(definition);
        }

        return Optional.empty();
    }

    @Override
    @NonNull
    public List<TaskDefinition> loadByGroup(@NonNull String taskGroup) {
        return loadAll().stream()
            .filter(d -> d.taskGroup().equals(taskGroup))
            .toList();
    }

    @Override
    public void save(@NonNull TaskDefinition definition) {
        String key = configPrefix + "." + definition.taskId();
        Map<String, Object> config = toConfigMap(definition);

        // 使用 register 注册配置
        configRegistry.register(key, config);

        log.info("Saved task definition: taskId={}", definition.taskId());

        // 通知监听器
        notifyListeners(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.UPDATED, definition);
    }

    @Override
    public void delete(@NonNull String taskId) {
        String key = configPrefix + "." + taskId;
        configRegistry.unregister(key);

        log.info("Deleted task definition: taskId={}", taskId);

        // 通知监听器
        notifyListeners(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.DELETED, null);
    }

    @Override
    public void addChangeListener(DynamicTaskConfigSource.@NonNull ConfigChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeChangeListener(DynamicTaskConfigSource.@NonNull ConfigChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void refresh() {
        // 配置中心自动刷新，这里主要是通知监听器
        for (DynamicTaskConfigSource.ConfigChangeListener listener : listeners) {
            listener.onConfigChange(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.REFRESHED, null);
        }

        log.info("Task config source refreshed");
    }

    @Override
    @NonNull
    public String getName() {
        return "config-center";
    }

    /**
     * 处理配置变更
     */
    private void handleConfigChange(@Nullable ConfigChangeEvent event) {
        if (event == null) {
            return;
        }

        String taskId = extractTaskId(event.prefix());
        if (taskId == null) {
            return;
        }

        TaskDefinition definition = null;
        DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent eventType;

        if (event.newValue() != null && event.newValue() instanceof Map) {
            @SuppressWarnings("unchecked")
            TaskDefinition parsed = parseTaskDefinition(taskId, (Map<String, Object>) event.newValue());
            definition = parsed;
            eventType = event.oldValue() == null ?
                DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.CREATED :
                DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.UPDATED;
        } else {
            eventType = DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent.DELETED;
        }

        notifyListeners(eventType, definition);
    }

    /**
     * 通知监听器
     */
    private void notifyListeners(DynamicTaskConfigSource.ConfigChangeListener.ConfigChangeEvent eventType, @Nullable TaskDefinition definition) {
        for (DynamicTaskConfigSource.ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChange(eventType, definition);
            } catch (Exception e) {
                log.error("Failed to notify config change listener: {}", e.getMessage());
            }
        }
    }

    /**
     * 提取任务 ID
     */
    @Nullable
    private String extractTaskId(@Nullable String key) {
        if (key == null || !key.startsWith(configPrefix + ".")) {
            return null;
        }

        String suffix = key.substring(configPrefix.length() + 1);
        // 只提取第一级作为任务 ID
        int dotIndex = suffix.indexOf('.');
        if (dotIndex > 0) {
            return suffix.substring(0, dotIndex);
        }
        return suffix;
    }

    /**
     * 解析任务定义
     */
    @Nullable
    private TaskDefinition parseTaskDefinition(@NonNull String taskId, @NonNull Map<String, Object> config) {
        try {
            String group = getString(config, "group", "default");
            String cron = getString(config, "cron", null);
            long fixedRate = getLong(config, "fixedRate", -1);
            long fixedDelay = getLong(config, "fixedDelay", -1);
            long initialDelay = getLong(config, "initialDelay", 0);
            String description = getString(config, "description", null);
            boolean enabled = getBoolean(config, "enabled", true);
            long timeout = getLong(config, "timeout", -1);
            int maxRetries = getInt(config, "maxRetries", 0);
            long retryDelay = getLong(config, "retryDelay", 0);

            return new TaskDefinition(
                taskId, group, cron, fixedRate, fixedDelay, initialDelay,
                description, enabled, timeout, maxRetries, retryDelay, null
            );

        } catch (Exception e) {
            log.error("Failed to parse task definition for {}: {}", taskId, e.getMessage());
            return null;
        }
    }

    /**
     * 转换为配置 Map
     */
    @NonNull
    private Map<String, Object> toConfigMap(@NonNull TaskDefinition definition) {
        Map<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("group", definition.taskGroup());
        if (definition.cron() != null) {
            config.put("cron", definition.cron());
        }
        if (definition.fixedRate() > 0) {
            config.put("fixedRate", definition.fixedRate());
        }
        if (definition.fixedDelay() > 0) {
            config.put("fixedDelay", definition.fixedDelay());
        }
        if (definition.initialDelay() > 0) {
            config.put("initialDelay", definition.initialDelay());
        }
        if (definition.description() != null) {
            config.put("description", definition.description());
        }
        config.put("enabled", definition.enabled());
        if (definition.timeout() > 0) {
            config.put("timeout", definition.timeout());
        }
        if (definition.maxRetries() > 0) {
            config.put("maxRetries", definition.maxRetries());
        }
        if (definition.retryDelay() > 0) {
            config.put("retryDelay", definition.retryDelay());
        }
        if (definition.metadata() != null) {
            config.put("metadata", definition.metadata());
        }
        return config;
    }

    private String getString(Map<String, Object> config, String key, String defaultValue) {
        Object value = config.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private long getLong(Map<String, Object> config, String key, long defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> config, String key, int defaultValue) {
        Object value = config.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}