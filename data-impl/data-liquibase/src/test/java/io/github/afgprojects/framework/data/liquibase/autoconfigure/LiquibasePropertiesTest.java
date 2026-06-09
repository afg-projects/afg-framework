package io.github.afgprojects.framework.data.liquibase.autoconfigure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LiquibaseProperties 单元测试
 * <p>
 * 验证默认值和 setter/getter 行为。
 */
class LiquibasePropertiesTest {

    // ========== 默认值 ==========

    @Nested
    @DisplayName("默认值")
    class DefaultValues {

        @Test
        @DisplayName("should return true when isEnabled called with defaults")
        void shouldReturnTrue_whenIsEnabledCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should return default changelog path when getChangeLog called with defaults")
        void shouldReturnDefaultChangelogPath_whenGetChangeLogCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getChangeLog()).isEqualTo("classpath:db/changelog/changelog.xml");
        }

        @Test
        @DisplayName("should return false when isDropFirst called with defaults")
        void shouldReturnFalse_whenIsDropFirstCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.isDropFirst()).isFalse();
        }

        @Test
        @DisplayName("should return null when getContexts called with defaults")
        void shouldReturnNull_whenGetContextsCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getContexts()).isNull();
        }

        @Test
        @DisplayName("should return null when getLabels called with defaults")
        void shouldReturnNull_whenGetLabelsCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getLabels()).isNull();
        }

        @Test
        @DisplayName("should return null when getDefaultSchema called with defaults")
        void shouldReturnNull_whenGetDefaultSchemaCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getDefaultSchema()).isNull();
        }

        @Test
        @DisplayName("should return DB_CHANGELOG when getDatabaseChangeLogTable called with defaults")
        void shouldReturnDbChangelog_whenGetDatabaseChangeLogTableCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getDatabaseChangeLogTable()).isEqualTo("DB_CHANGELOG");
        }

        @Test
        @DisplayName("should return DB_CHANGELOG_LOCK when getDatabaseChangeLogLockTable called with defaults")
        void shouldReturnDbChangelogLock_whenGetDatabaseChangeLogLockTableCalledWithDefaults() {
            LiquibaseProperties properties = new LiquibaseProperties();

            assertThat(properties.getDatabaseChangeLogLockTable()).isEqualTo("DB_CHANGELOG_LOCK");
        }
    }

    // ========== Setter/Getter ==========

    @Nested
    @DisplayName("Setter/Getter")
    class SetterGetter {

        @Test
        @DisplayName("should set enabled when setEnabled called")
        void shouldSetEnabled_whenSetEnabledCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should set changeLog when setChangeLog called")
        void shouldSetChangeLog_whenSetChangeLogCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setChangeLog("classpath:db/my-changelog.xml");

            assertThat(properties.getChangeLog()).isEqualTo("classpath:db/my-changelog.xml");
        }

        @Test
        @DisplayName("should set dropFirst when setDropFirst called")
        void shouldSetDropFirst_whenSetDropFirstCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setDropFirst(true);

            assertThat(properties.isDropFirst()).isTrue();
        }

        @Test
        @DisplayName("should set contexts when setContexts called")
        void shouldSetContexts_whenSetContextsCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setContexts("dev,prod");

            assertThat(properties.getContexts()).isEqualTo("dev,prod");
        }

        @Test
        @DisplayName("should set labels when setLabels called")
        void shouldSetLabels_whenSetLabelsCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setLabels("v1.0.0,v1.1.0");

            assertThat(properties.getLabels()).isEqualTo("v1.0.0,v1.1.0");
        }

        @Test
        @DisplayName("should set defaultSchema when setDefaultSchema called")
        void shouldSetDefaultSchema_whenSetDefaultSchemaCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setDefaultSchema("public");

            assertThat(properties.getDefaultSchema()).isEqualTo("public");
        }

        @Test
        @DisplayName("should set databaseChangeLogTable when setDatabaseChangeLogTable called")
        void shouldSetDatabaseChangeLogTable_whenSetDatabaseChangeLogTableCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setDatabaseChangeLogTable("MY_CHANGELOG");

            assertThat(properties.getDatabaseChangeLogTable()).isEqualTo("MY_CHANGELOG");
        }

        @Test
        @DisplayName("should set databaseChangeLogLockTable when setDatabaseChangeLogLockTable called")
        void shouldSetDatabaseChangeLogLockTable_whenSetDatabaseChangeLogLockTableCalled() {
            LiquibaseProperties properties = new LiquibaseProperties();
            properties.setDatabaseChangeLogLockTable("MY_CHANGELOG_LOCK");

            assertThat(properties.getDatabaseChangeLogLockTable()).isEqualTo("MY_CHANGELOG_LOCK");
        }
    }
}