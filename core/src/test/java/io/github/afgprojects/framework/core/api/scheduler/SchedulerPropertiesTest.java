package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SchedulerProperties 测试
 */
@DisplayName("SchedulerProperties 测试")
class SchedulerPropertiesTest {

    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties props = new SchedulerProperties();

            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getDefaultTimeout()).isEqualTo(Duration.ofMinutes(30));
            assertThat(props.getDefaultRetryAttempts()).isEqualTo(3);
            assertThat(props.getDefaultRetryDelay()).isEqualTo(Duration.ofSeconds(1));
            assertThat(props.getRetryMultiplier()).isEqualTo(2.0);
            assertThat(props.getThreadPoolSize()).isPositive();
            assertThat(props.getLogStorage()).isNotNull();
            assertThat(props.getMetrics()).isNotNull();
            assertThat(props.getDynamicTask()).isNotNull();
            assertThat(props.getAnnotations()).isNotNull();
            assertThat(props.getApi()).isNotNull();
        }
    }

    @Nested
    @DisplayName("LogStorageConfig 测试")
    class LogStorageConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.LogStorageConfig config = new SchedulerProperties.LogStorageConfig();

            assertThat(config.getType()).isEqualTo("memory");
            assertThat(config.getMaxSize()).isEqualTo(10000);
            assertThat(config.getRetention()).isEqualTo(Duration.ofDays(7));
            assertThat(config.isLogSuccess()).isTrue();
            assertThat(config.isLogErrorStack()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.LogStorageConfig config = new SchedulerProperties.LogStorageConfig();
            config.setType("redis");
            config.setMaxSize(5000);
            config.setRetention(Duration.ofDays(14));
            config.setLogSuccess(false);
            config.setLogErrorStack(false);

            assertThat(config.getType()).isEqualTo("redis");
            assertThat(config.getMaxSize()).isEqualTo(5000);
            assertThat(config.getRetention()).isEqualTo(Duration.ofDays(14));
            assertThat(config.isLogSuccess()).isFalse();
            assertThat(config.isLogErrorStack()).isFalse();
        }
    }

    @Nested
    @DisplayName("MetricsConfig 测试")
    class MetricsConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.MetricsConfig config = new SchedulerProperties.MetricsConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getPrefix()).isEqualTo("afg.scheduler");
            assertThat(config.getTags()).isEmpty();
            assertThat(config.isRecordDurationHistogram()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.MetricsConfig config = new SchedulerProperties.MetricsConfig();
            config.setEnabled(false);
            config.setPrefix("custom.scheduler");
            Map<String, String> tags = new HashMap<>();
            tags.put("app", "test");
            config.setTags(tags);
            config.setRecordDurationHistogram(false);

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getPrefix()).isEqualTo("custom.scheduler");
            assertThat(config.getTags()).containsEntry("app", "test");
            assertThat(config.isRecordDurationHistogram()).isFalse();
        }
    }

    @Nested
    @DisplayName("DynamicTaskConfig 测试")
    class DynamicTaskConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.DynamicTaskConfig config = new SchedulerProperties.DynamicTaskConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getSourceType()).isEqualTo("config-center");
            assertThat(config.getRefreshInterval()).isEqualTo(Duration.ofMinutes(1));
            assertThat(config.getConfigPrefix()).isEqualTo("afg.tasks");
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.DynamicTaskConfig config = new SchedulerProperties.DynamicTaskConfig();
            config.setEnabled(true);
            config.setSourceType("jdbc");
            config.setRefreshInterval(Duration.ofMinutes(5));
            config.setConfigPrefix("custom.tasks");

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getSourceType()).isEqualTo("jdbc");
            assertThat(config.getRefreshInterval()).isEqualTo(Duration.ofMinutes(5));
            assertThat(config.getConfigPrefix()).isEqualTo("custom.tasks");
        }
    }

    @Nested
    @DisplayName("AnnotationConfig 测试")
    class AnnotationConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.AnnotationConfig config = new SchedulerProperties.AnnotationConfig();

            assertThat(config.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.AnnotationConfig config = new SchedulerProperties.AnnotationConfig();
            config.setEnabled(false);

            assertThat(config.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("ApiConfig 测试")
    class ApiConfigTests {

        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.ApiConfig config = new SchedulerProperties.ApiConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getBasePath()).isEqualTo("/afg/scheduler");
        }

        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.ApiConfig config = new SchedulerProperties.ApiConfig();
            config.setEnabled(true);
            config.setBasePath("/custom/scheduler");

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getBasePath()).isEqualTo("/custom/scheduler");
        }
    }
}
