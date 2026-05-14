package io.github.afgprojects.framework.core.api.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link SchedulerProperties} 调度器配置属性测试
 *
 * <p>测试调度器的各项配置及其嵌套配置类：
 * <ul>
 *   <li>默认值验证</li>
 *   <li>LogStorageConfig - 日志存储配置</li>
 *   <li>MetricsConfig - 指标配置</li>
 *   <li>DynamicTaskConfig - 动态任务配置</li>
 *   <li>AnnotationConfig - 注解配置</li>
 *   <li>ApiConfig - API 配置</li>
 * </ul>
 *
 * @see SchedulerProperties
 */
@DisplayName("SchedulerProperties 测试")
class SchedulerPropertiesTest {

    /**
     * 主配置默认值测试
     */
    @Nested
    @DisplayName("默认值测试")
    class DefaultValueTests {

        /**
         * 验证调度器主配置的默认值
         */
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

    /**
     * LogStorageConfig 日志存储配置测试
     */
    @Nested
    @DisplayName("LogStorageConfig 测试")
    class LogStorageConfigTests {

        /**
         * 验证日志存储配置的默认值
         */
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

        /**
         * 验证日志存储配置的属性设置
         */
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

    /**
     * MetricsConfig 指标配置测试
     */
    @Nested
    @DisplayName("MetricsConfig 测试")
    class MetricsConfigTests {

        /**
         * 验证指标配置的默认值
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.MetricsConfig config = new SchedulerProperties.MetricsConfig();

            assertThat(config.isEnabled()).isTrue();
            assertThat(config.getPrefix()).isEqualTo("afg.scheduler");
            assertThat(config.getTags()).isEmpty();
            assertThat(config.isRecordDurationHistogram()).isTrue();
        }

        /**
         * 验证指标配置的属性设置
         */
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

    /**
     * DynamicTaskConfig 动态任务配置测试
     */
    @Nested
    @DisplayName("DynamicTaskConfig 测试")
    class DynamicTaskConfigTests {

        /**
         * 验证动态任务配置的默认值
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.DynamicTaskConfig config = new SchedulerProperties.DynamicTaskConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getSourceType()).isEqualTo("config-center");
            assertThat(config.getRefreshInterval()).isEqualTo(Duration.ofMinutes(1));
            assertThat(config.getConfigPrefix()).isEqualTo("afg.tasks");
        }

        /**
         * 验证动态任务配置的属性设置
         */
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

    /**
     * AnnotationConfig 注解配置测试
     */
    @Nested
    @DisplayName("AnnotationConfig 测试")
    class AnnotationConfigTests {

        /**
         * 验证注解配置的默认值
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.AnnotationConfig config = new SchedulerProperties.AnnotationConfig();

            assertThat(config.isEnabled()).isTrue();
        }

        /**
         * 验证注解配置的属性设置
         */
        @Test
        @DisplayName("应该正确设置属性")
        void shouldSetProperties() {
            SchedulerProperties.AnnotationConfig config = new SchedulerProperties.AnnotationConfig();
            config.setEnabled(false);

            assertThat(config.isEnabled()).isFalse();
        }
    }

    /**
     * ApiConfig API 配置测试
     */
    @Nested
    @DisplayName("ApiConfig 测试")
    class ApiConfigTests {

        /**
         * 验证 API 配置的默认值
         */
        @Test
        @DisplayName("应该有正确的默认值")
        void shouldHaveCorrectDefaultValues() {
            SchedulerProperties.ApiConfig config = new SchedulerProperties.ApiConfig();

            assertThat(config.isEnabled()).isFalse();
            assertThat(config.getBasePath()).isEqualTo("/afg/scheduler");
        }

        /**
         * 验证 API 配置的属性设置
         */
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
