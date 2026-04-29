package io.github.afgprojects.framework.core.support;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis 容器单例管理器
 * 确保所有集成测试共享同一个 Redis 容器实例
 */
public final class RedisContainerSingleton {

    private static final GenericContainer<?> INSTANCE;
    private static boolean started = false;

    static {
        INSTANCE = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);
    }

    private RedisContainerSingleton() {}

    /**
     * 获取 Redis 容器实例
     * 首次调用时启动容器
     */
    public static GenericContainer<?> getInstance() {
        if (!started) {
            INSTANCE.start();
            started = true;
        }
        return INSTANCE;
    }

    /**
     * 获取 Redis 主机地址
     */
    public static String getHost() {
        return getInstance().getHost();
    }

    /**
     * 获取 Redis 端口
     */
    public static int getPort() {
        return getInstance().getMappedPort(6379);
    }
}
