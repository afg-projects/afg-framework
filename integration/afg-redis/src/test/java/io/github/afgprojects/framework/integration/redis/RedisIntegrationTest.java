package io.github.afgprojects.framework.integration.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 集成测试
 * <p>
 * 使用 Testcontainers 启动 Redis 容器进行集成测试。
 * </p>
 */
@Testcontainers
@DisplayName("Redis 集成测试")
class RedisIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Test
    @DisplayName("应该成功连接到 Redis")
    void shouldConnectToRedis() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        RedissonClient client = Redisson.create(config);

        assertThat(client).isNotNull();
        client.shutdown();
    }

    @Test
    @DisplayName("应该成功执行 PING 命令")
    void shouldExecutePingCommand() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        RedissonClient client = Redisson.create(config);

        // Redisson 通过 getNodes() 可以验证连接
        assertThat(client.getNodesGroup().getNodes()).isNotEmpty();

        client.shutdown();
    }

    @Test
    @DisplayName("应该成功执行基本字符串操作")
    void shouldExecuteBasicStringOperations() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        RedissonClient client = Redisson.create(config);

        var bucket = client.getBucket("test-key");
        bucket.set("test-value");

        assertThat(bucket.get()).isEqualTo("test-value");
        bucket.delete();

        client.shutdown();
    }

    @Test
    @DisplayName("应该成功执行基本 Map 操作")
    void shouldExecuteBasicMapOperations() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redis.getHost() + ":" + redis.getMappedPort(6379));

        RedissonClient client = Redisson.create(config);

        var map = client.getMap("test-map");
        map.put("key1", "value1");
        map.put("key2", "value2");

        assertThat(map.get("key1")).isEqualTo("value1");
        assertThat(map.size()).isEqualTo(2);
        map.delete();

        client.shutdown();
    }
}
