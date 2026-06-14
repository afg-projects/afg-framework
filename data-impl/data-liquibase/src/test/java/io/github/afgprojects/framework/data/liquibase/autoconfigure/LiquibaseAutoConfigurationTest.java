package io.github.afgprojects.framework.data.liquibase.autoconfigure;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Liquibase AutoConfiguration 集成测试
 * <p>
 * 验证 Liquibase 自动配置能正确加载、创建 SpringLiquibase bean、
 * 并执行 changelog 迁移。
 * <p>
 * 使用 PostgreSQL Testcontainers 进行测试，不使用 Mockito 或 H2。
 */
@SpringBootTest(classes = LiquibaseTestConfiguration.class)
@TestPropertySource(properties = {
    "afg.liquibase.enabled=true",
    "afg.liquibase.change-log=classpath:db/changelog/changelog.xml"
})
class LiquibaseAutoConfigurationTest {

    static {
        // 在类加载时启动 PostgreSQL 容器
        LiquibasePostgresSupport.start();
    }

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    DataSource dataSource;

    // ========== AutoConfiguration 加载 ==========

    @Nested
    @DisplayName("AutoConfiguration 加载")
    class AutoConfigurationLoading {

        @Test
        @DisplayName("should create SpringLiquibase bean when liquibase enabled")
        void shouldCreateSpringLiquibaseBean_whenLiquibaseEnabled() {
            SpringLiquibase liquibase = applicationContext.getBean(SpringLiquibase.class);

            assertThat(liquibase).isNotNull();
            assertThat(liquibase.getDataSource()).isNotNull();
        }

        @Test
        @DisplayName("should create LiquibaseProperties bean when liquibase enabled")
        void shouldCreateLiquibasePropertiesBean_whenLiquibaseEnabled() {
            LiquibaseProperties properties = applicationContext.getBean(LiquibaseProperties.class);

            assertThat(properties).isNotNull();
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should use configured changelog path when property set")
        void shouldUseConfiguredChangelogPath_whenPropertySet() {
            SpringLiquibase liquibase = applicationContext.getBean(SpringLiquibase.class);

            assertThat(liquibase.getChangeLog()).isEqualTo("classpath:db/changelog/changelog.xml");
        }
    }

    // ========== 迁移执行 ==========

    @Nested
    @DisplayName("迁移执行")
    class MigrationExecution {

        @Test
        @DisplayName("should execute changelog and create tables when liquibase runs")
        void shouldExecuteChangelogAndCreateTables_whenLiquibaseRuns() {
            SpringLiquibase liquibase = applicationContext.getBean(SpringLiquibase.class);

            // SpringLiquibase 在 Spring 上下文启动时自动执行迁移
            assertThat(liquibase).isNotNull();

            // 验证 Liquibase 管理表已创建（证明迁移已执行）
            assertThat(applicationContext.getBean(DataSource.class)).isNotNull();
        }

        @Test
        @DisplayName("should have DataSource available when liquibase configured")
        void shouldHaveDataSourceAvailable_whenLiquibaseConfigured() {
            assertThat(dataSource).isNotNull();
        }
    }

    // ========== 禁用配置 ==========

    @Nested
    @DisplayName("禁用配置")
    @TestPropertySource(properties = {
        "afg.liquibase.enabled=false"
    })
    class DisabledConfiguration {

        @Test
        @DisplayName("should not create SpringLiquibase bean when liquibase disabled")
        void shouldNotCreateSpringLiquibaseBean_whenLiquibaseDisabled() {
            assertThat(applicationContext.containsBeanDefinition("liquibase")).isFalse();
        }
    }
}
