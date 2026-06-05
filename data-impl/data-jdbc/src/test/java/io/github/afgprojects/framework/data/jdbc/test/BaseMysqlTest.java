package io.github.afgprojects.framework.data.jdbc.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.afgprojects.framework.data.core.DataManager;

/**
 * MySQL Testcontainers 测试基类
 * <p>
 * 使用真实的 MySQL 容器，用于验证方言兼容性、批量操作等场景。
 * 容器在所有测试间共享（static @Container），测试后自动清理。
 * </p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
public abstract class BaseMysqlTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("afg_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    protected DataManager dataManager;

    @org.springframework.beans.factory.annotation.Autowired
    public void setDataManager(DataManager dataManager) {
        this.dataManager = dataManager;
    }
}
