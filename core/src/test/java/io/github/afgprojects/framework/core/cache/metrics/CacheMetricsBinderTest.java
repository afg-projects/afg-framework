package io.github.afgprojects.framework.core.cache.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * CacheMetricsBinder 测试
 */
@DisplayName("CacheMetricsBinder 测试")
class CacheMetricsBinderTest {

    private CacheMetrics metrics;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        metrics = new CacheMetrics("test-cache", "local");
        meterRegistry = new SimpleMeterRegistry();
    }

    @Nested
    @DisplayName("bind 测试")
    class BindTests {

        @Test
        @DisplayName("应该绑定缓存指标到 MeterRegistry")
        void shouldBindMetricsToMeterRegistry() {
            // 记录一些操作
            metrics.recordHit();
            metrics.recordMiss();
            metrics.recordPut();

            // when
            CacheMetricsBinder.bind(metrics, meterRegistry);

            // then
            assertThat(meterRegistry.find("afg.cache.hits").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.misses").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.puts").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.evictions").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.loads").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.load.failures").counter()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.hit.rate").gauge()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.miss.rate").gauge()).isNotNull();
            assertThat(meterRegistry.find("afg.cache.load.success.rate").gauge()).isNotNull();
        }

        @Test
        @DisplayName("应该正确设置标签")
        void shouldSetCorrectTags() {
            // when
            CacheMetricsBinder.bind(metrics, meterRegistry);

            // then
            var hitCounter = meterRegistry.find("afg.cache.hits").counter();
            assertThat(hitCounter).isNotNull();
            assertThat(hitCounter.getId().getTag("cache")).isEqualTo("test-cache");
            assertThat(hitCounter.getId().getTag("type")).isEqualTo("local");
        }
    }
}
