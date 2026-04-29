package io.github.afgprojects.framework.core.feature;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;

/**
 * 功能开关管理器
 * <p>
 * 管理功能开关状态，支持内存存储和分布式存储（Redisson）。
 * 线程安全，支持读写锁。
 * </p>
 */
public class FeatureFlagManager {

    private static final Logger log = LoggerFactory.getLogger(FeatureFlagManager.class);

    /**
     * 功能开关存储（内存模式）
     */
    private final Map<String, FeatureFlag> featureFlags = new ConcurrentHashMap<>();

    /**
     * 读写锁，保证线程安全
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 配置属性
     */
    private final FeatureFlagProperties properties;

    /**
     * 分布式存储客户端（可选）
     */
    private final @Nullable DistributedStorageClient storageClient;

    /**
     * 构造函数（内存模式）
     *
     * @param properties 配置属性
     */
    public FeatureFlagManager(@NonNull FeatureFlagProperties properties) {
        this(properties, null);
    }

    /**
     * 构造函数（分布式模式）
     *
     * @param properties     配置属性
     * @param storageClient  分布式存储客户端
     */
    public FeatureFlagManager(
            @NonNull FeatureFlagProperties properties, @Nullable DistributedStorageClient storageClient) {
        this.properties = properties;
        this.storageClient = storageClient;
    }

    /**
     * 判断功能是否启用
     *
     * @param featureName 功能名称
     * @return 是否启用
     */
    public boolean isEnabled(@NonNull String featureName) {
        return isEnabled(featureName, getCurrentContext());
    }

    /**
     * 判断功能是否对指定上下文启用
     *
     * @param featureName 功能名称
     * @param context     灰度上下文
     * @return 是否启用
     */
    public boolean isEnabled(@NonNull String featureName, @NonNull GrayscaleContext context) {
        FeatureFlag flag = getFeatureFlag(featureName);
        if (flag == null) {
            // 功能未配置，使用注解的默认值（由切面处理）
            return true;
        }
        return flag.isEnabledFor(context);
    }

    /**
     * 判断功能是否启用（带默认值）
     *
     * @param featureName      功能名称
     * @param enabledByDefault 默认值
     * @return 是否启用
     */
    public boolean isEnabled(@NonNull String featureName, boolean enabledByDefault) {
        return isEnabled(featureName, getCurrentContext(), enabledByDefault);
    }

    /**
     * 判断功能是否对指定上下文启用（带默认值）
     *
     * @param featureName      功能名称
     * @param context          灰度上下文
     * @param enabledByDefault 默认值
     * @return 是否启用
     */
    public boolean isEnabled(
            @NonNull String featureName, @NonNull GrayscaleContext context, boolean enabledByDefault) {
        FeatureFlag flag = getFeatureFlag(featureName);
        if (flag == null) {
            return enabledByDefault;
        }
        return flag.isEnabledFor(context);
    }

    /**
     * 获取功能开关
     *
     * @param featureName 功能名称
     * @return 功能开关，不存在则返回 null
     */
    public @Nullable FeatureFlag getFeatureFlag(@NonNull String featureName) {
        // 先尝试从分布式存储获取
        if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
            FeatureFlag flag = storageClient.get(featureName);
            if (flag != null) {
                return flag;
            }
        }

        // 从内存存储获取
        lock.readLock().lock();
        try {
            return featureFlags.get(featureName);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 注册功能开关
     *
     * @param flag 功能开关
     */
    public void register(@NonNull FeatureFlag flag) {
        lock.writeLock().lock();
        try {
            featureFlags.put(flag.name(), flag);
            log.info("功能开关已注册: {} -> {}", flag.name(), flag.enabled());

            // 同步到分布式存储
            if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
                storageClient.put(flag.name(), flag);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 批量注册功能开关
     *
     * @param flags 功能开关集合
     */
    public void registerAll(@NonNull Collection<FeatureFlag> flags) {
        lock.writeLock().lock();
        try {
            for (FeatureFlag flag : flags) {
                featureFlags.put(flag.name(), flag);
            }
            log.info("批量注册功能开关: {} 个", flags.size());

            // 同步到分布式存储
            if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
                Map<String, FeatureFlag> flagMap = new HashMap<>();
                for (FeatureFlag flag : flags) {
                    flagMap.put(flag.name(), flag);
                }
                storageClient.putAll(flagMap);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 启用功能
     *
     * @param featureName 功能名称
     */
    public void enable(@NonNull String featureName) {
        setEnabled(featureName, true);
    }

    /**
     * 禁用功能
     *
     * @param featureName 功能名称
     */
    public void disable(@NonNull String featureName) {
        setEnabled(featureName, false);
    }

    /**
     * 设置功能启用状态
     *
     * @param featureName 功能名称
     * @param enabled     是否启用
     */
    public void setEnabled(@NonNull String featureName, boolean enabled) {
        lock.writeLock().lock();
        try {
            FeatureFlag existing = featureFlags.get(featureName);
            FeatureFlag flag;
            if (existing != null) {
                // 更新现有功能开关
                // 当 enabled 状态改变时，同步更新 grayscaleRule
                GrayscaleRule rule = existing.grayscaleRule();
                if (enabled && rule == GrayscaleRule.NONE) {
                    rule = GrayscaleRule.ALL;
                } else if (!enabled && rule == GrayscaleRule.ALL) {
                    rule = GrayscaleRule.NONE;
                }
                flag = new FeatureFlag(
                        featureName,
                        enabled,
                        rule,
                        existing.description(),
                        existing.createdAt(),
                        java.time.Instant.now(),
                        existing.updatedBy());
            } else {
                // 创建新功能开关
                flag = FeatureFlag.of(featureName, enabled);
            }
            featureFlags.put(featureName, flag);
            log.info("功能开关状态已更新: {} -> {}", featureName, enabled);

            // 同步到分布式存储
            if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
                storageClient.put(featureName, flag);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 设置灰度规则
     *
     * @param featureName 功能名称
     * @param rule        灰度规则
     */
    public void setGrayscaleRule(@NonNull String featureName, @NonNull GrayscaleRule rule) {
        lock.writeLock().lock();
        try {
            FeatureFlag existing = featureFlags.get(featureName);
            FeatureFlag flag;
            if (existing != null) {
                flag = new FeatureFlag(
                        featureName,
                        existing.enabled(),
                        rule,
                        existing.description(),
                        existing.createdAt(),
                        java.time.Instant.now(),
                        existing.updatedBy());
            } else {
                flag = FeatureFlag.of(featureName, rule);
            }
            featureFlags.put(featureName, flag);
            log.info("功能开关灰度规则已更新: {} -> {}", featureName, rule);

            // 同步到分布式存储
            if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
                storageClient.put(featureName, flag);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 删除功能开关
     *
     * @param featureName 功能名称
     * @return 被删除的功能开关
     */
    public @Nullable FeatureFlag remove(@NonNull String featureName) {
        lock.writeLock().lock();
        try {
            FeatureFlag removed = featureFlags.remove(featureName);
            if (removed != null) {
                log.info("功能开关已删除: {}", featureName);

                // 从分布式存储删除
                if (storageClient != null && properties.getStorageType() != FeatureFlagProperties.StorageType.MEMORY) {
                    storageClient.remove(featureName);
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取所有功能开关
     *
     * @return 功能开关集合
     */
    @NonNull
    public Collection<FeatureFlag> getAllFeatureFlags() {
        lock.readLock().lock();
        try {
            return new HashMap<>(featureFlags).values();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 刷新功能开关（从分布式存储重新加载）
     */
    public void refresh() {
        if (storageClient == null || properties.getStorageType() == FeatureFlagProperties.StorageType.MEMORY) {
            return;
        }

        Map<String, FeatureFlag> allFlags = storageClient.getAll();
        if (allFlags != null && !allFlags.isEmpty()) {
            lock.writeLock().lock();
            try {
                featureFlags.clear();
                featureFlags.putAll(allFlags);
                log.info("功能开关已刷新: {} 个", allFlags.size());
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * 获取当前请求的灰度上下文
     *
     * @return 灰度上下文
     */
    @NonNull
    private GrayscaleContext getCurrentContext() {
        return GrayscaleContext.builder()
                .userId(AfgRequestContextHolder.getUserId())
                .tenantId(AfgRequestContextHolder.getTenantId())
                .username(AfgRequestContextHolder.getUsername())
                .clientIp(AfgRequestContextHolder.getClientIp())
                .build();
    }

    /**
     * 分布式存储客户端接口
     */
    public interface DistributedStorageClient {

        /**
         * 获取功能开关
         *
         * @param featureName 功能名称
         * @return 功能开关
         */
        @Nullable
        FeatureFlag get(@NonNull String featureName);

        /**
         * 存储功能开关
         *
         * @param featureName 功能名称
         * @param flag        功能开关
         */
        void put(@NonNull String featureName, @NonNull FeatureFlag flag);

        /**
         * 批量存储功能开关
         *
         * @param flags 功能开关映射
         */
        void putAll(@NonNull Map<String, FeatureFlag> flags);

        /**
         * 删除功能开关
         *
         * @param featureName 功能名称
         */
        void remove(@NonNull String featureName);

        /**
         * 获取所有功能开关
         *
         * @return 功能开关映射
         */
        @NonNull
        Map<String, FeatureFlag> getAll();
    }
}