package io.github.afgprojects.framework.core.config;

/**
 * 配置来源枚举
 * 按优先级从低到高排列
 */
public enum ConfigSource {

    /** 模块默认配置（最低优先级） */
    MODULE_DEFAULT(0),

    /** 依赖模块配置文件 */
    DEPENDENCY_CONFIG(1),

    /** 当前模块配置文件 */
    CURRENT_CONFIG(2),

    /** 环境变量 */
    ENVIRONMENT(3),

    /** 命令行参数 */
    COMMAND_LINE(4),

    /** 配置中心（最高优先级，支持动态刷新） */
    CONFIG_CENTER(5);

    private final int priority;

    ConfigSource(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 是否支持动态刷新
     */
    public boolean supportsRefresh() {
        return this == CONFIG_CENTER;
    }
}
