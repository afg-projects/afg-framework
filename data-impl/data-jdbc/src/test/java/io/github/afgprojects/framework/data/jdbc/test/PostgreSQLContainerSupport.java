package io.github.afgprojects.framework.data.jdbc.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL Testcontainers 单例
 *
 * <p>确保所有测试类共享同一个容器实例，避免重复启动。
 * 使用 withReuse(true) 跨 JVM 进程复用容器。
 *
 * <p>首次调用 {@link #start()} 时启动容器并设置 System Properties，
 * 后续调用为 no-op。
 */
public final class PostgreSQLContainerSupport {

    private static final PostgreSQLContainer<?> CONTAINER = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine"))
        .withReuse(true);

    private static volatile boolean started = false;

    private PostgreSQLContainerSupport() {
    }

    public static PostgreSQLContainer<?> getInstance() {
        return CONTAINER;
    }

    /**
     * 启动容器并设置 Spring DataSource 系统属性。
     * 多次调用安全（幂等）。
     */
    public static synchronized void start() {
        if (!started) {
            CONTAINER.start();
            System.setProperty("spring.datasource.url", CONTAINER.getJdbcUrl());
            System.setProperty("spring.datasource.username", CONTAINER.getUsername());
            System.setProperty("spring.datasource.password", CONTAINER.getPassword());
            started = true;
        }
    }
}
