package io.github.afgprojects.framework.core.cloud;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;

/**
 * 存活探针端点
 *
 * <p>检测应用是否存活，如果检测失败 Kubernetes 会重启容器
 *
 * @since 1.0.0
 */
public class LivenessProbeEndpoint {

    private final KubernetesProbeProperties.ProbeConfig config;
    private final AtomicBoolean healthy = new AtomicBoolean(true);

    public LivenessProbeEndpoint(KubernetesProbeProperties.ProbeConfig config) {
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
     * 检查存活状态
     *
     * @return 是否存活
     */
    public boolean isHealthy() {
        return healthy.get();
    }

    /**
     * 设置存活状态
     *
     * @param healthy 是否存活
     */
    public void setHealthy(boolean healthy) {
        this.healthy.set(healthy);
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