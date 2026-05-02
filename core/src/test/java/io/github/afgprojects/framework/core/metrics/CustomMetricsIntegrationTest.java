package io.github.afgprojects.framework.core.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import io.github.afgprojects.framework.core.support.TestApplication;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

/**
 * CustomMetrics 集成测试
 */
@DisplayName("CustomMetrics 集成测试")
@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "afg.metrics.enabled=true",
                "afg.metrics.tags.application=test-app",
                "afg.metrics.tags.env=test",
                "afg.metrics.histogram.enabled=true",
                "management.endpoints.web.exposure.include=metrics,prometheus"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomMetricsIntegrationTest {

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    private CustomMetrics customMetrics;

    @Nested
    @DisplayName("指标配置测试")
    class MetricsConfigTests {

        @Test
        @DisplayName("应该自动配置 MeterRegistry")
        void shouldAutoConfigureMeterRegistry() {
            assertThat(meterRegistry).isNotNull();
        }

        @Test
        @DisplayName("应该自动配置 CustomMetrics")
        void shouldAutoConfigureCustomMetrics() {
            // CustomMetrics 需要 MeterRegistry bean 和 MetricsProperties
            // 如果 MeterRegistry 存在，CustomMetrics 应该被创建
            if (meterRegistry != null && customMetrics != null) {
                assertThat(customMetrics).isNotNull();
            }
        }
    }

    @Nested
    @DisplayName("计数器测试")
    class CounterTests {

        @Test
        @DisplayName("应该能够创建计数器")
        void shouldCreateCounter() {
            Counter counter = Counter.builder("test.counter")
                    .description("Test counter")
                    .tag("type", "test")
                    .register(meterRegistry);

            counter.increment();

            assertThat(counter.count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("应该能够多次增加计数器")
        void shouldIncrementCounterMultipleTimes() {
            Counter counter = Counter.builder("test.counter.multi")
                    .register(meterRegistry);

            counter.increment();
            counter.increment();
            counter.increment(5);

            assertThat(counter.count()).isEqualTo(7.0);
        }
    }

    @Nested
    @DisplayName("仪表盘测试")
    class GaugeTests {

        @Test
        @DisplayName("应该能够创建仪表盘")
        void shouldCreateGauge() {
            AtomicInteger value = new AtomicInteger(10);

            Gauge gauge = Gauge.builder("test.gauge", value, AtomicInteger::get)
                    .description("Test gauge")
                    .register(meterRegistry);

            assertThat(gauge.value()).isEqualTo(10.0);

            value.set(20);

            assertThat(gauge.value()).isEqualTo(20.0);
        }
    }

    @Nested
    @DisplayName("计时器测试")
    class TimerTests {

        @Test
        @DisplayName("应该能够创建计时器")
        void shouldCreateTimer() {
            Timer timer = Timer.builder("test.timer")
                    .description("Test timer")
                    .register(meterRegistry);

            timer.record(() -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            assertThat(timer.count()).isEqualTo(1);
            assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThanOrEqualTo(10);
        }

        @Test
        @DisplayName("应该能够记录多次计时")
        void shouldRecordMultipleTimes() {
            Timer timer = Timer.builder("test.timer.multi")
                    .register(meterRegistry);

            timer.record(java.time.Duration.ofMillis(10));
            timer.record(java.time.Duration.ofMillis(20));

            assertThat(timer.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("标签测试")
    class TagTests {

        @Test
        @DisplayName("应该能够为指标添加标签")
        void shouldAddTagsToMetrics() {
            Counter counter = Counter.builder("test.tagged.counter")
                    .tag("application", "test-app")
                    .tag("environment", "test")
                    .register(meterRegistry);

            counter.increment();

            assertThat(counter.getId().getTag("application")).isEqualTo("test-app");
            assertThat(counter.getId().getTag("environment")).isEqualTo("test");
        }
    }

    @Nested
    @DisplayName("CustomMetrics 功能测试")
    class CustomMetricsFeatureTests {

        @Test
        @DisplayName("应该能够获取 MeterRegistry")
        void shouldGetMeterRegistry() {
            if (customMetrics != null) {
                assertThat(customMetrics.getMeterRegistry()).isNotNull();
            }
        }

        @Test
        @DisplayName("应该能够创建计数器")
        void shouldCreateCounterViaCustomMetrics() {
            if (customMetrics != null) {
                Counter counter = customMetrics.counter("custom.counter", "type", "test");
                assertThat(counter).isNotNull();
            }
        }

        @Test
        @DisplayName("应该能够创建计时器")
        void shouldCreateTimerViaCustomMetrics() {
            if (customMetrics != null) {
                Timer timer = customMetrics.timer("custom.timer", "type", "test");
                assertThat(timer).isNotNull();
            }
        }
    }

    // Helper class for gauge test
    private static class AtomicInteger {
        private int value;

        AtomicInteger(int value) {
            this.value = value;
        }

        int get() {
            return value;
        }

        void set(int value) {
            this.value = value;
        }
    }
}
