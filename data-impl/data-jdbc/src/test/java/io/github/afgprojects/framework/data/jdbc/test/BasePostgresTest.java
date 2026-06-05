package io.github.afgprojects.framework.data.jdbc.test;

import io.github.afgprojects.framework.data.jdbc.JdbcDataTestConfiguration;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * PostgreSQL Testcontainers 测试基类
 *
 * <p>使用 static PostgreSQL 容器，在类加载时启动，所有测试类共享。
 * Spring 上下文通过 classes = JdbcDataTestConfiguration.class 实现缓存复用，
 * 避免每个测试类启动新的容器和上下文。
 *
 * <p>容器使用 withReuse(true)，配合 ~/.testcontainers.properties 中
 * testcontainers.reuse.enable=true，跨 JVM 进程复用容器。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = JdbcDataTestConfiguration.class)
public abstract class BasePostgresTest {

    static {
        // 在类加载时启动容器并设置 DataSource 属性，确保 Spring 上下文创建时容器已就绪
        PostgreSQLContainerSupport.start();
    }
}
