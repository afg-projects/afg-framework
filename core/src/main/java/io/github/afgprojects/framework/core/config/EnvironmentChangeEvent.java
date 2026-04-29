package io.github.afgprojects.framework.core.config;

/**
 * 环境切换事件
 */
public record EnvironmentChangeEvent(String oldEnvironment, String newEnvironment, long timestamp) {

    public EnvironmentChangeEvent(String oldEnvironment, String newEnvironment) {
        this(oldEnvironment, newEnvironment, System.currentTimeMillis());
    }
}
