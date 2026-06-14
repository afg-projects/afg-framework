package io.github.afgprojects.framework.integration.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Redis Testcontainers 单例支持
 *
 * <p>确保所有 Redis 集成测试共享同一个容器实例和 RedissonClient。
 * 使用 static GenericContainer，在类加载时启动，所有测试类共享。
 * 容器使用 withReuse(true)，配合 ~/.testcontainers.properties 中
 * testcontainers.reuse.enable=true，跨 JVM 进程复用容器。
 */
public final class RedisContainerSupport {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private static final GenericContainer<?> CONTAINER = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(6379)
            .withReuse(true);

    private static volatile RedissonClient redissonClient;

    private static volatile boolean started = false;

    private RedisContainerSupport() {
    }

    /**
     * 获取容器实例
     *
     * @return Redis 容器
     */
    public static GenericContainer<?> getInstance() {
        return CONTAINER;
    }

    /**
     * 启动容器并创建 RedissonClient。
     * 多次调用安全（幂等）。
     */
    public static synchronized void start() {
        if (!started) {
            CONTAINER.start();
            started = true;
        }
    }

    /**
     * 获取共享的 RedissonClient 实例
     *
     * @return RedissonClient
     */
    public static synchronized RedissonClient getRedissonClient() {
        start();
        if (redissonClient == null || redissonClient.isShutdown()) {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://" + CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(6379))
                    .setConnectTimeout(5000)
                    .setTimeout(5000);
            redissonClient = Redisson.create(config);
        }
        return redissonClient;
    }

    /**
     * 获取 Redis 地址
     *
     * @return host:port
     */
    public static String getRedisAddress() {
        start();
        return CONTAINER.getHost() + ":" + CONTAINER.getMappedPort(6379);
    }
}
