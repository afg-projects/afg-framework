package io.github.afgprojects.framework.data.liquibase;

import io.github.afgprojects.framework.data.liquibase.runner.LiquibaseMigrationRunner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MigrationService 单元测试
 * <p>
 * 验证 MigrationService 的工厂方法和基本行为。
 * 不依赖数据库连接，仅测试对象创建和方法签名。
 */
class MigrationServiceTest {

    // ========== 工厂方法 ==========

    @Nested
    @DisplayName("工厂方法")
    class FactoryMethods {

        @Test
        @DisplayName("should create MigrationService for H2 when forH2 called")
        void shouldCreateMigrationServiceForH2_whenForH2Called() {
            MigrationService service = MigrationService.forH2();

            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("should create MigrationService for MySQL when forMySQL called")
        void shouldCreateMigrationServiceForMySQL_whenForMySQLCalled() {
            MigrationService service = MigrationService.forMySQL();

            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("should create MigrationService for PostgreSQL when forPostgreSQL called")
        void shouldCreateMigrationServiceForPostgreSQL_whenForPostgreSQLCalled() {
            MigrationService service = MigrationService.forPostgreSQL();

            assertThat(service).isNotNull();
        }
    }

    // ========== LiquibaseMigrationRunner ==========

    @Nested
    @DisplayName("LiquibaseMigrationRunner.MigrationStatus")
    class MigrationStatusTest {

        @Test
        @DisplayName("should create MigrationStatus record with correct values")
        void shouldCreateMigrationStatus_withCorrectValues() {
            LiquibaseMigrationRunner.MigrationStatus status =
                    new LiquibaseMigrationRunner.MigrationStatus(
                            "v1.0.0-001-sys-user", "afg", "createTable", false);

            assertThat(status.changeSetId()).isEqualTo("v1.0.0-001-sys-user");
            assertThat(status.author()).isEqualTo("afg");
            assertThat(status.description()).isEqualTo("createTable");
            assertThat(status.executed()).isFalse();
        }
    }
}