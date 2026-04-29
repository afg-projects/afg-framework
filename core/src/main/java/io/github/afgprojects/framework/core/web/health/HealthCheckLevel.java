package io.github.afgprojects.framework.core.web.health;

/**
 * 健康检查级别枚举
 * 定义不同深度的健康检查，支持 Kubernetes 探针
 *
 * @since 1.0.0
 */
public enum HealthCheckLevel {

    /**
     * 存活检查（Liveness Probe）
     * 检查应用是否运行中，若失败则 Kubernetes 会重启容器
     * 检查内容：
     * <ul>
     *   <li>JVM 状态</li>
     *   <li>线程死锁检测</li>
     * </ul>
     */
    LIVENESS(1, "存活检查"),

    /**
     * 就绪检查（Readiness Probe）
     * 检查应用是否可对外提供服务，若失败则 Kubernetes 会将 Pod 标记为不可用
     * 检查内容：
     * <ul>
     *   <li>数据库连接</li>
     *   <li>Redis 连接</li>
     *   <li>模块状态</li>
     * </ul>
     */
    READINESS(2, "就绪检查"),

    /**
     * 深度检查（Deep Health Check）
     * 包含所有依赖服务的健康状态
     * 检查内容：
     * <ul>
     *   <li>所有 LIVENESS 检查项</li>
     *   <li>所有 READINESS 检查项</li>
     *   <li>外部依赖服务</li>
     *   <li>配置中心连接</li>
     * </ul>
     */
    DEEP(3, "深度检查");

    private final int level;
    private final String description;

    HealthCheckLevel(int level, String description) {
        this.level = level;
        this.description = description;
    }

    /**
     * 获取检查级别数值
     *
     * @return 级别数值，数值越大检查越深入
     */
    public int getLevel() {
        return level;
    }

    /**
     * 获取级别描述
     *
     * @return 中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 判断当前级别是否包含指定级别的检查
     *
     * @param other 其他检查级别
     * @return 如果当前级别大于等于指定级别，返回 true
     */
    public boolean includes(HealthCheckLevel other) {
        return this.level >= other.level;
    }
}
