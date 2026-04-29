package io.github.afgprojects.framework.core.cloud;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;

/**
 * 就绪探针端点
 *
 * <p>检测应用是否准备好接收流量，如果检测失败 Kubernetes 会将 Pod 从 Service 端点中移除
 *
 * @since 1.0.0
 */
public class ReadinessProbeEndpoint {

    private final KubernetesProbeProperties.ProbeConfig config;
    private final AtomicBoolean ready = new AtomicBoolean(false);

    public ReadinessProbeEndpoint(KubernetesProbeProperties.ProbeConfig config) {
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
     * 检查就绪状态
     *
     * @return 是否就绪
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * 设置就绪状态
     *
     * @param ready 是否就绪
     */
    public void setReady(boolean ready) {
        this.ready.set(ready);
    }

    /**
     * 标记为就绪
     */
    public void markReady() {
        this.ready.set(true);
    }

    /**
     * 标记为未就绪
     */
    public void markNotReady() {
        this.ready.set(false);
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