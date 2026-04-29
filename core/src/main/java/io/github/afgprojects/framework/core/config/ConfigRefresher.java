package io.github.afgprojects.framework.core.config;

/**
 * 配置刷新器
 * 配置刷新的统一入口，委托给 AfgConfigRegistry 执行
 */
public class ConfigRefresher {

    private final AfgConfigRegistry registry;

    /**
     * @param registry 配置注册中心
     * @throws IllegalArgumentException 如果registry为null
     */
    public ConfigRefresher(AfgConfigRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("Registry cannot be null");
        }
        this.registry = registry;
    }

    /**
     * 刷新配置（更新缓存值并通知监听器）
     *
     * @param prefix    配置前缀
     * @param newConfig 新配置对象
     * @throws IllegalArgumentException 如果配置不存在或prefix为null
     */
    public void refresh(String prefix, Object newConfig) {
        if (prefix == null) {
            throw new IllegalArgumentException("Prefix cannot be null");
        }
        registry.updateConfig(prefix, newConfig);
    }

    /**
     * 从配置中心刷新配置
     *
     * @param prefix    配置前缀
     * @param newConfig 新配置值
     * @throws IllegalStateException    如果该前缀没有 CONFIG_CENTER 来源的配置
     * @throws IllegalArgumentException 如果配置不存在
     */
    public void refreshFromConfigCenter(String prefix, Object newConfig) {
        registry.refreshFromConfigCenter(prefix, newConfig);
    }

    /**
     * 获取指定前缀的生效配置来源
     *
     * @param prefix 配置前缀
     * @return 配置来源，不存在返回 null
     */
    public ConfigSource getActiveSource(String prefix) {
        return registry.getActiveSource(prefix);
    }

    /**
     * 获取关联的配置注册中心
     */
    public AfgConfigRegistry getRegistry() {
        return registry;
    }
}
