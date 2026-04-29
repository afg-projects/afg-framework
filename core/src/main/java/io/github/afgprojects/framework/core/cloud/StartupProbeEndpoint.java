package io.github.afgprojects.framework.core.cloud;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;

/**
 * 启动探针端点
 *
 * <p>检测应用是否启动完成，用于慢启动应用的保护
 *
 * @since 1.0.0
 */
public class StartupProbeEndpoint {

    private final KubernetesProbeProperties.ProbeConfig config;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public StartupProbeEndpoint(KubernetesProbeProperties.ProbeConfig config) {
        this.config = config;
    }

    /**
     * 获取探针路径
     *
     * @return 探针路径
     */
    @NonNull
    public String getPath() {
        return config.getPath();
    }

    /**
     * 检查启动状态
     *
     * @return 是否启动完成
     */
    public boolean isStarted() {
        return started.get();
    }

    /**
     * 设置启动状态
     *
     * @param started 是否启动完成
     */
    public void setStarted(boolean started) {
        this.started.set(started);
    }

    /**
     * 标记为启动完成
     */
    public void markStarted() {
        this.started.set(true);
    }

    /**
     * 获取探针配置
     *
     * @return 探针配置
     */
    public KubernetesProbeProperties.@NonNull ProbeConfig getConfig() {
        return config;
    }
}