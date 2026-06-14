package io.github.afgprojects.framework.integration.redis;

import org.redisson.api.RedissonClient;

/**
 * Redis 集成测试基类
 *
 * <p>使用 RedisContainerSupport 单例容器，在类加载时启动，所有测试类共享。
 * 每个测试类继承此基类即可获得 RedissonClient。
 */
public abstract class BaseRedisTest {

    static {
        // 在类加载时启动容器并设置 RedissonClient，确保测试执行时容器已就绪
        RedisContainerSupport.start();
    }

    /**
     * 获取共享的 RedissonClient 实例
     *
     * @return RedissonClient
     */
    protected static RedissonClient getRedissonClient() {
        return RedisContainerSupport.getRedissonClient();
    }

    /**
     * 获取 Redis 地址
     *
     * @return host:port
     */
    protected static String getRedisAddress() {
        return RedisContainerSupport.getRedisAddress();
    }
}
