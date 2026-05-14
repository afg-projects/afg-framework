package io.github.afgprojects.framework.core.web.health.spi;

/**
 * Redis 健康检查接口
 * <p>
 * 定义 Redis 连接健康检查的标准接口。
 */
public interface RedisHealthChecker {

    /**
     * 检查 Redis 连接是否健康
     *
     * @return 健康检查结果
     */
    RedisHealthResult check();

    /**
     * 获取检查器名称
     */
    String getName();
}
