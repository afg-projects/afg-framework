package io.github.afgprojects.framework.core.support;

import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import io.github.afgprojects.framework.core.util.JacksonUtils;

/**
 * 集成测试基类（带 Testcontainers 支持）
 * 加载完整的 Spring 上下文进行集成测试，并自动启动 Redis 容器
 *
 * <p>使用单例容器模式，确保所有测试类共享同一个容器实例，
 * 避免并行测试时重复启动容器导致的资源浪费和冲突。
 *
 * <p>使用 @DirtiesContext 确保每个测试类完成后上下文被清理。
 * 同时重置静态状态（如 JacksonUtils）以避免测试间干扰。
 */
@SpringBootTest(classes = TestApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @Autowired
    protected ApplicationContext applicationContext;

    /**
     * 动态配置 Redis 连接属性
     * 使用单例容器管理器获取共享的 Redis 容器
     */
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", RedisContainerSingleton::getHost);
        registry.add("spring.data.redis.port", RedisContainerSingleton::getPort);
        registry.add("spring.data.redis.password", () -> "");
    }

    /**
     * 获取 Spring Bean
     *
     * @param clazz Bean 类型
     * @param <T>   类型参数
     * @return Bean 实例
     */
    protected <T> T getBean(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    /**
     * 获取 Spring Bean（按名称）
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    protected Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    /**
     * 重置静态状态（配合 @DirtiesContext 使用）
     * 由于 @DirtiesContext 会销毁 Spring 上下文，
     * 需要重置 JacksonUtils 以便下一个测试类可以重新初始化。
     */
    @AfterAll
    static void resetStaticState() {
        JacksonUtils.reset();
    }
}
