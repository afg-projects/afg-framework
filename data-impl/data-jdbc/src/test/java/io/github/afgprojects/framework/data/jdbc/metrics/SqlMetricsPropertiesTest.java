package io.github.afgprojects.framework.data.jdbc.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SqlMetricsProperties 单元测试
 */
@DisplayName("SqlMetricsProperties 测试")
class SqlMetricsPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SqlMetricsProperties properties = new SqlMetricsProperties();

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(1000));
            assertThat(properties.isLogSlowQueries()).isTrue();
            assertThat(properties.getMaxSlowQueryLogs()).isEqualTo(100);
            assertThat(properties.isLogSqlParams()).isFalse();
            assertThat(properties.getTags()).isNotNull();
        }

        @Test
        @DisplayName("标签配置应该有正确的默认值")
        void shouldHaveCorrectDefaultTagsConfig() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            SqlMetricsProperties.TagsConfig tags = properties.getTags();

            assertThat(tags.isIncludeEntity()).isTrue();
            assertThat(tags.isIncludeOperation()).isTrue();
            assertThat(tags.isIncludeDataSource()).isFalse();
        }
    }

    @Nested
    @DisplayName("Setter 测试")
    class SetterTests {

        @Test
        @DisplayName("应该正确设置 enabled 属性")
        void shouldSetEnabledProperty() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 slowQueryThreshold 属性")
        void shouldSetSlowQueryThresholdProperty() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setSlowQueryThreshold(Duration.ofMillis(5000));

            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(5000));
        }

        @Test
        @DisplayName("应该正确设置 logSlowQueries 属性")
        void shouldSetLogSlowQueriesProperty() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setLogSlowQueries(false);

            assertThat(properties.isLogSlowQueries()).isFalse();
        }

        @Test
        @DisplayName("应该正确设置 maxSlowQueryLogs 属性")
        void shouldSetMaxSlowQueryLogsProperty() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setMaxSlowQueryLogs(500);

            assertThat(properties.getMaxSlowQueryLogs()).isEqualTo(500);
        }

        @Test
        @DisplayName("应该正确设置 logSqlParams 属性")
        void shouldSetLogSqlParamsProperty() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setLogSqlParams(true);

            assertThat(properties.isLogSqlParams()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置 tags 配置")
        void shouldSetTagsConfig() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            SqlMetricsProperties.TagsConfig tags = new SqlMetricsProperties.TagsConfig();
            tags.setIncludeEntity(false);
            tags.setIncludeOperation(false);
            tags.setIncludeDataSource(true);
            properties.setTags(tags);

            assertThat(properties.getTags().isIncludeEntity()).isFalse();
            assertThat(properties.getTags().isIncludeOperation()).isFalse();
            assertThat(properties.getTags().isIncludeDataSource()).isTrue();
        }
    }

    @Nested
    @DisplayName("TagsConfig 测试")
    class TagsConfigTests {

        @Test
        @DisplayName("TagsConfig 应该有独立的属性值")
        void shouldHaveIndependentPropertyValues() {
            SqlMetricsProperties.TagsConfig tags1 = new SqlMetricsProperties.TagsConfig();
            SqlMetricsProperties.TagsConfig tags2 = new SqlMetricsProperties.TagsConfig();

            tags1.setIncludeEntity(false);
            tags2.setIncludeEntity(true);

            assertThat(tags1.isIncludeEntity()).isFalse();
            assertThat(tags2.isIncludeEntity()).isTrue();
        }
    }

    @Nested
    @DisplayName("配置场景测试")
    class ConfigurationScenarioTests {

        @Test
        @DisplayName("应该支持生产环境配置")
        void shouldSupportProductionConfiguration() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setEnabled(true);
            properties.setSlowQueryThreshold(Duration.ofMillis(2000));
            properties.setLogSlowQueries(true);
            properties.setMaxSlowQueryLogs(1000);
            properties.setLogSqlParams(false);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(2000));
            assertThat(properties.isLogSlowQueries()).isTrue();
            assertThat(properties.getMaxSlowQueryLogs()).isEqualTo(1000);
            assertThat(properties.isLogSqlParams()).isFalse();
        }

        @Test
        @DisplayName("应该支持开发环境配置")
        void shouldSupportDevelopmentConfiguration() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setEnabled(true);
            properties.setSlowQueryThreshold(Duration.ofMillis(500));
            properties.setLogSlowQueries(true);
            properties.setLogSqlParams(true);

            assertThat(properties.isEnabled()).isTrue();
            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(500));
            assertThat(properties.isLogSqlParams()).isTrue();
        }

        @Test
        @DisplayName("应该支持禁用指标配置")
        void shouldSupportDisabledConfiguration() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setEnabled(false);

            assertThat(properties.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("边界值测试")
    class BoundaryTests {

        @Test
        @DisplayName("应该允许设置非常短的慢查询阈值")
        void shouldAllowVeryShortSlowQueryThreshold() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setSlowQueryThreshold(Duration.ofMillis(10));

            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMillis(10));
        }

        @Test
        @DisplayName("应该允许设置非常长的慢查询阈值")
        void shouldAllowVeryLongSlowQueryThreshold() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setSlowQueryThreshold(Duration.ofMinutes(10));

            assertThat(properties.getSlowQueryThreshold()).isEqualTo(Duration.ofMinutes(10));
        }

        @Test
        @DisplayName("应该允许设置 maxSlowQueryLogs 为 0")
        void shouldAllowZeroMaxSlowQueryLogs() {
            SqlMetricsProperties properties = new SqlMetricsProperties();
            properties.setMaxSlowQueryLogs(0);

            assertThat(properties.getMaxSlowQueryLogs()).isEqualTo(0);
        }
    }
}