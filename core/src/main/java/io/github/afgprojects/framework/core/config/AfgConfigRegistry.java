package io.github.afgprojects.framework.core.config;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AFG 配置注册中心
 * 管理所有模块配置，默认启用读写锁保证线程安全
 */
@SuppressWarnings({"PMD.AvoidUsingVolatile", "PMD.CommentDefaultAccessModifier"})
public class AfgConfigRegistry {

    private static final Logger log = LoggerFactory.getLogger(AfgConfigRegistry.class);

    private final SourceManager sourceManager = new SourceManager();
    private final ListenerManager listenerManager = new ListenerManager();

    // NOPMD - volatile 用于线程安全是合理的
    private volatile @Nullable String activeEnvironment;

    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    public AfgConfigRegistry() {
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        this.readLock = rwLock.readLock();
        this.writeLock = rwLock.writeLock();
    }

    public void register(@NonNull String prefix, @NonNull Object config) {
        register(prefix, config, ConfigSource.MODULE_DEFAULT);
    }

    public void register(@NonNull String prefix, @NonNull Object config, @NonNull ConfigSource source) {
        withWriteLock(() -> {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("Prefix cannot be null or blank");
            }
            if (config == null) {
                throw new IllegalArgumentException("Config cannot be null");
            }
            if (source == null) {
                throw new IllegalArgumentException("Source cannot be null");
            }

            sourceManager.registerSource(prefix, config, source);
            log.info("Config registered: {} from source: {}", prefix, source);
        });
    }

    public void unregister(@Nullable String prefix) {
        withWriteLock(() -> {
            if (prefix != null) {
                sourceManager.removeSources(prefix);
                listenerManager.removeListeners(prefix);
                log.info("Config unregistered: {}", prefix);
            }
        });
    }

    public boolean contains(@NonNull String prefix) {
        return withReadLock(() -> sourceManager.contains(prefix));
    }

    public @Nullable Object getConfig(@NonNull String prefix) {
        return withReadLock(() -> sourceManager.getConfig(prefix));
    }

    @SuppressWarnings("unchecked")
    public @Nullable <T> T getConfig(@NonNull String prefix, @NonNull Class<T> clazz) {
        return withReadLock(() -> {
            Object config = sourceManager.getConfig(prefix);
            if (clazz.isInstance(config)) {
                return (T) config;
            }
            if (config != null && !clazz.isInstance(config)) {
                throw new ClassCastException("Cannot cast " + config.getClass().getName() + " to " + clazz.getName());
            }
            return null;
        });
    }

    public void updateConfig(@NonNull String prefix, @NonNull Object newConfig) {
        withWriteLock(() -> {
            if (!sourceManager.contains(prefix)) {
                throw new IllegalArgumentException("Config with prefix [" + prefix + "] does not exist");
            }
            Object oldConfig = sourceManager.getConfig(prefix);
            sourceManager.updateConfig(prefix, newConfig);
            listenerManager.notifyListeners(prefix, oldConfig, newConfig, ConfigSource.CURRENT_CONFIG);
        });
    }

    public void addListener(@NonNull String prefix, @NonNull ConfigChangeListener listener) {
        withWriteLock(() -> listenerManager.addListener(prefix, listener));
    }

    public void removeListener(@NonNull String prefix, @NonNull ConfigChangeListener listener) {
        withWriteLock(() -> listenerManager.removeListener(prefix, listener));
    }

    public boolean hasListeners(@NonNull String prefix) {
        return withReadLock(() -> listenerManager.hasListeners(prefix));
    }

    @NonNull public Map<String, Object> getAllConfigs() {
        return withReadLock(sourceManager::getAllConfigs);
    }

    @NonNull public List<ConfigEntry> getConfigSources(@NonNull String prefix) {
        return withReadLock(() -> sourceManager.getConfigSources(prefix));
    }

    public @Nullable ConfigEntry getFinalConfigEntry(@NonNull String prefix) {
        return withReadLock(() -> sourceManager.getFinalConfigEntry(prefix));
    }

    public @Nullable ConfigSource getActiveSource(@NonNull String prefix) {
        return withReadLock(() -> sourceManager.getActiveSource(prefix));
    }

    public void setActiveEnvironment(@Nullable String environment) {
        withWriteLock(() -> {
            String oldEnvironment = this.activeEnvironment;
            this.activeEnvironment = environment;
            log.info("Environment changed: {} -> {}", oldEnvironment, environment);
        });
    }

    public @Nullable String getActiveEnvironment() {
        return withReadLock(() -> activeEnvironment);
    }

    public void refreshFromConfigCenter(@NonNull String prefix, @NonNull Object newConfig) {
        withWriteLock(() -> {
            Object oldConfig = sourceManager.getConfig(prefix);
            sourceManager.refreshFromConfigCenter(prefix, newConfig);
            log.info("Config refreshed from config center: {}", prefix);
            listenerManager.notifyListeners(prefix, oldConfig, newConfig, ConfigSource.CONFIG_CENTER);
        });
    }

    private <T> T withReadLock(Supplier<T> action) {
        readLock.lock();
        try {
            return action.get();
        } finally {
            readLock.unlock();
        }
    }

    private <T> T withWriteLock(Supplier<T> action) {
        writeLock.lock();
        try {
            return action.get();
        } finally {
            writeLock.unlock();
        }
    }

    private void withWriteLock(Runnable action) {
        writeLock.lock();
        try {
            action.run();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * 多源配置管理
     */
    private final class SourceManager {

        private final Map<String, Object> configs = new ConcurrentHashMap<>();
        private final Map<String, Map<ConfigSource, ConfigEntry>> configSources = new ConcurrentHashMap<>();

        void registerSource(@NonNull String prefix, @NonNull Object config, @NonNull ConfigSource source) {
            ConfigEntry entry = ConfigEntry.builder()
                    .source(source)
                    .prefix(prefix)
                    .value(config)
                    .loadedAtNow()
                    .build();

            configSources
                    .computeIfAbsent(prefix, k -> new ConcurrentHashMap<>())
                    .put(source, entry);

            updateCache(prefix);
        }

        void removeSources(@NonNull String prefix) {
            configSources.remove(prefix);
            configs.remove(prefix);
        }

        boolean contains(@NonNull String prefix) {
            return configs.containsKey(prefix);
        }

        @Nullable Object getConfig(@NonNull String prefix) {
            return configs.get(prefix);
        }

        void updateConfig(@NonNull String prefix, @NonNull Object newConfig) {
            configs.put(prefix, newConfig);
        }

        @NonNull Map<String, Object> getAllConfigs() {
            return Collections.unmodifiableMap(configs);
        }

        @NonNull List<ConfigEntry> getConfigSources(@NonNull String prefix) {
            Map<ConfigSource, ConfigEntry> sources = configSources.get(prefix);
            if (sources == null || sources.isEmpty()) {
                return Collections.emptyList();
            }
            return sources.values().stream()
                    .sorted((e1, e2) -> Integer.compare(
                            e2.source().getPriority(), e1.source().getPriority()))
                    .collect(Collectors.toList());
        }

        @Nullable ConfigEntry getFinalConfigEntry(@NonNull String prefix) {
            Map<ConfigSource, ConfigEntry> sources = configSources.get(prefix);
            if (sources == null || sources.isEmpty()) {
                return null;
            }
            return sources.values().stream()
                    .max((e1, e2) -> Integer.compare(
                            e1.source().getPriority(), e2.source().getPriority()))
                    .orElse(null);
        }

        @Nullable ConfigSource getActiveSource(@NonNull String prefix) {
            ConfigEntry entry = getFinalConfigEntry(prefix);
            return entry != null ? entry.source() : null;
        }

        void refreshFromConfigCenter(@NonNull String prefix, @NonNull Object newConfig) {
            Map<ConfigSource, ConfigEntry> sources = configSources.get(prefix);
            if (sources == null) {
                throw new IllegalArgumentException("Config with prefix [" + prefix + "] does not exist");
            }

            ConfigEntry entry = sources.get(ConfigSource.CONFIG_CENTER);
            if (entry == null) {
                throw new IllegalStateException("Config with prefix [" + prefix + "] is not from CONFIG_CENTER source");
            }

            ConfigEntry newEntry = ConfigEntry.builder()
                    .source(ConfigSource.CONFIG_CENTER)
                    .prefix(prefix)
                    .value(newConfig)
                    .loadedAtNow()
                    .build();

            sources.put(ConfigSource.CONFIG_CENTER, newEntry);
            updateCache(prefix);
        }

        private void updateCache(@NonNull String prefix) {
            Map<ConfigSource, ConfigEntry> sources = configSources.get(prefix);
            if (sources == null || sources.isEmpty()) {
                configs.remove(prefix);
                return;
            }
            sources.values().stream()
                    .max((e1, e2) -> Integer.compare(
                            e1.source().getPriority(), e2.source().getPriority()))
                    .ifPresent(highestPriorityEntry -> configs.put(prefix, highestPriorityEntry.value()));
        }
    }

    /**
     * 监听器管理
     */
    private final class ListenerManager {

        private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();

        void addListener(@NonNull String prefix, @NonNull ConfigChangeListener listener) {
            listeners
                    .computeIfAbsent(prefix, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(listener);
        }

        void removeListener(@NonNull String prefix, @NonNull ConfigChangeListener listener) {
            List<ConfigChangeListener> listenerList = listeners.get(prefix);
            if (listenerList != null) {
                listenerList.remove(listener);
            }
        }

        void removeListeners(@NonNull String prefix) {
            listeners.remove(prefix);
        }

        boolean hasListeners(@NonNull String prefix) {
            List<ConfigChangeListener> listenerList = listeners.get(prefix);
            return listenerList != null && !listenerList.isEmpty();
        }

        void notifyListeners(
                @NonNull String prefix,
                @Nullable Object oldConfig,
                @NonNull Object newConfig,
                @NonNull ConfigSource source) {
            List<ConfigChangeListener> listenerList = listeners.get(prefix);
            if (listenerList != null) {
                ConfigChangeEvent event = ConfigChangeEvent.update(prefix, oldConfig, newConfig, source);
                for (ConfigChangeListener listener : listenerList) {
                    listener.onConfigChange(event);
                }
            }
        }
    }
}
