package io.github.afgprojects.framework.data.jdbc;

import static org.assertj.core.api.Assertions.*;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.jdbc.test.BaseDataTest;

/**
 * DataManager 自动配置集成测试
 * <p>
 * 使用 PostgreSQL Testcontainers + Liquibase 迁移。
 * 验证 Spring Boot 自动装配的 DataManager Bean 是否正确注入。
 * </p>
 */
class DataManagerAutoConfigurationTest extends BaseDataTest {

    @Nested
    @DisplayName("Bean 装配")
    class BeanAssembly {

        @Test
        @DisplayName("should inject DataManager when auto-configuration is enabled")
        void shouldInjectDataManager_whenAutoConfigurationEnabled() {
            assertThat(dataManager).isNotNull();
        }

        @Test
        @DisplayName("should inject JdbcDataManager when DataManager is requested")
        void shouldInjectJdbcDataManager_whenDataManagerRequested() {
            assertThat(dataManager).isInstanceOf(JdbcDataManager.class);
        }
    }

    @Nested
    @DisplayName("数据源")
    class DataSourceVerification {

        @Autowired
        DataSource dataSource;

        @Test
        @DisplayName("should provide PostgreSQL data source when Testcontainers is configured")
        void shouldProvidePostgresDataSource_whenTestcontainersConfigured() {
            assertThat(dataSource).isNotNull();
        }
    }
}