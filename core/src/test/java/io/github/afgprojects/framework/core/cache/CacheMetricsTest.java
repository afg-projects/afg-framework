package io.github.afgprojects.framework.core.cache;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.afgprojects.framework.core.cache.metrics.CacheMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CacheMetrics 单元测试。
 * <p>
 * 测试缓存指标的功能，包括基本属性、指标记录、比率计算、重置和字符串表示。
 * </p>
 *
 * @see CacheMetrics
 */
@DisplayName("CacheMetrics 测试")
class CacheMetricsTest {

    private CacheMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new CacheMetrics("test-cache", "local");
    }

    /**
     * 基本属性测试。
     * <p>
     * 测试缓存名称和缓存类型的获取。
     * </p>
     */
    @Nested
    @DisplayName("基本属性测试")
    class BasicPropertyTests {

        /**
         * 测试返回缓存名称。
         */
        @Test
        @DisplayName("应该返回缓存名称")
        void shouldReturnCacheName() {
            assertThat(metrics.getCacheName()).isEqualTo("test-cache");
        }

        /**
         * 测试返回缓存类型。
         */
        @Test
        @DisplayName("应该返回缓存类型")
        void shouldReturnCacheType() {
            assertThat(metrics.getCacheType()).isEqualTo("local");
        }
    }

    /**
     * 指标记录测试。
     * <p>
     * 测试各种缓存操作的指标记录，包括获取、命中、未命中、存入、删除和加载。
     * </p>
     */
    @Nested
    @DisplayName("指标记录测试")
    class RecordMetricsTests {

        /**
         * 测试正确记录获取次数。
         */
        @Test
        @DisplayName("应该正确记录获取次数")
        void shouldRecordGet() {
            // when
            metrics.recordGet();
            metrics.recordGet();
            metrics.recordGet();

            // then
            assertThat(metrics.getGetCount()).isEqualTo(3);
        }

        /**
         * 测试正确记录命中次数。
         */
        @Test
        @DisplayName("应该正确记录命中次数")
        void shouldRecordHit() {
            // when
            metrics.recordHit();
            metrics.recordHit();

            // then
            assertThat(metrics.getHitCount()).isEqualTo(2);
        }

        /**
         * 测试正确记录未命中次数。
         */
        @Test
        @DisplayName("应该正确记录未命中次数")
        void shouldRecordMiss() {
            // when
            metrics.recordMiss();

            // then
            assertThat(metrics.getMissCount()).isEqualTo(1);
        }

        /**
         * 测试正确记录存入次数。
         */
        @Test
        @DisplayName("应该正确记录存入次数")
        void shouldRecordPut() {
            // when
            metrics.recordPut();
            metrics.recordPut();
            metrics.recordPut();
            metrics.recordPut();

            // then
            assertThat(metrics.getPutCount()).isEqualTo(4);
        }

        /**
         * 测试正确记录删除次数。
         */
        @Test
        @DisplayName("应该正确记录删除次数")
        void shouldRecordEviction() {
            // when
            metrics.recordEviction();

            // then
            assertThat(metrics.getEvictCount()).isEqualTo(1);
        }

        /**
         * 测试正确记录加载次数。
         */
        @Test
        @DisplayName("应该正确记录加载次数")
        void shouldRecordLoad() {
            // when
            metrics.recordLoad();
            metrics.recordLoad();

            // then
            assertThat(metrics.getLoadCount()).isEqualTo(2);
        }

        /**
         * 测试正确记录加载失败次数。
         */
        @Test
        @DisplayName("应该正确记录加载失败次数")
        void shouldRecordLoadFailure() {
            // when
            metrics.recordLoadFailure();

            // then
            assertThat(metrics.getLoadFailureCount()).isEqualTo(1);
        }
    }

    /**
     * 比率计算测试。
     * <p>
     * 测试命中率、未命中率和加载成功率的计算。
     * </p>
     */
    @Nested
    @DisplayName("比率计算测试")
    class RatioCalculationTests {

        /**
         * 测试正确计算命中率。
         */
        @Test
        @DisplayName("应该正确计算命中率")
        void shouldCalculateHitRate() {
            // given
            metrics.recordGet();
            metrics.recordGet();
            metrics.recordGet();
            metrics.recordHit();
            metrics.recordHit();

            // when
            double hitRate = metrics.getHitRate();

            // then
            assertThat(hitRate).isEqualTo(2.0 / 3.0);
        }

        /**
         * 测试无请求时命中率为 0。
         */
        @Test
        @DisplayName("无请求时命中率应该为 0")
        void shouldReturnZeroHitRateWhenNoRequests() {
            assertThat(metrics.getHitRate()).isEqualTo(0.0);
        }

        /**
         * 测试正确计算未命中率。
         */
        @Test
        @DisplayName("应该正确计算未命中率")
        void shouldCalculateMissRate() {
            // given
            metrics.recordGet();
            metrics.recordGet();
            metrics.recordHit();

            // when
            double missRate = metrics.getMissRate();

            // then
            assertThat(missRate).isEqualTo(0.5);
        }

        /**
         * 测试正确计算加载成功率。
         */
        @Test
        @DisplayName("应该正确计算加载成功率")
        void shouldCalculateLoadSuccessRate() {
            // given
            metrics.recordLoad();
            metrics.recordLoad();
            metrics.recordLoad();
            metrics.recordLoadFailure();

            // when
            double rate = metrics.getLoadSuccessRate();

            // then
            assertThat(rate).isEqualTo(3.0 / 4.0);
        }

        /**
         * 测试无加载时成功率为 1。
         */
        @Test
        @DisplayName("无加载时成功率应该为 1")
        void shouldReturnOneWhenNoLoads() {
            assertThat(metrics.getLoadSuccessRate()).isEqualTo(1.0);
        }
    }

    /**
     * 重置测试。
     * <p>
     * 测试重置后所有指标归零。
     * </p>
     */
    @Nested
    @DisplayName("重置测试")
    class ResetTests {

        /**
         * 测试重置后所有指标为 0。
         */
        @Test
        @DisplayName("重置后所有指标应该为 0")
        void shouldResetAllMetrics() {
            // given
            metrics.recordGet();
            metrics.recordHit();
            metrics.recordMiss();
            metrics.recordPut();
            metrics.recordEviction();
            metrics.recordLoad();
            metrics.recordLoadFailure();

            // when
            metrics.reset();

            // then
            assertThat(metrics.getGetCount()).isZero();
            assertThat(metrics.getHitCount()).isZero();
            assertThat(metrics.getMissCount()).isZero();
            assertThat(metrics.getPutCount()).isZero();
            assertThat(metrics.getEvictCount()).isZero();
            assertThat(metrics.getLoadCount()).isZero();
            assertThat(metrics.getLoadFailureCount()).isZero();
        }
    }

    /**
     * toString 测试。
     * <p>
     * 测试字符串表示包含关键信息。
     * </p>
     */
    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        /**
         * 测试输出有意义的字符串表示。
         */
        @Test
        @DisplayName("应该输出有意义的字符串表示")
        void shouldOutputMeaningfulString() {
            // given
            metrics.recordGet();
            metrics.recordHit();

            // when
            String str = metrics.toString();

            // then
            assertThat(str).contains("test-cache");
            assertThat(str).contains("local");
            assertThat(str).contains("gets=1");
            assertThat(str).contains("hits=1");
        }
    }
}