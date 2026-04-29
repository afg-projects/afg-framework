package io.github.afgprojects.framework.core.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.afgprojects.framework.core.trace.TracingProperties.Sampling;

/**
 * TracingSampler 测试类
 */
@DisplayName("TracingSampler 测试")
class TracingSamplerTest {

    @Test
    @DisplayName("ALWAYS 策略始终采样")
    void testAlwaysStrategy() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.ALWAYS);

        TracingSampler sampler = new TracingSampler(sampling);

        for (int i = 0; i < 10; i++) {
            assertThat(sampler.shouldSample()).isTrue();
        }
    }

    @Test
    @DisplayName("NEVER 策略从不采样")
    void testNeverStrategy() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.NEVER);

        TracingSampler sampler = new TracingSampler(sampling);

        for (int i = 0; i < 10; i++) {
            assertThat(sampler.shouldSample()).isFalse();
        }
    }

    @Test
    @DisplayName("概率采样概率为 1.0 时始终采样")
    void testProbabilityOne() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.PROBABILITY);
        sampling.setProbability(1.0);

        TracingSampler sampler = new TracingSampler(sampling);

        for (int i = 0; i < 10; i++) {
            assertThat(sampler.shouldSample()).isTrue();
        }
    }

    @Test
    @DisplayName("概率采样概率为 0.0 时从不采样")
    void testProbabilityZero() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.PROBABILITY);
        sampling.setProbability(0.0);

        TracingSampler sampler = new TracingSampler(sampling);

        for (int i = 0; i < 10; i++) {
            assertThat(sampler.shouldSample()).isFalse();
        }
    }

    @Test
    @DisplayName("概率采样概率为 0.5 时大约一半采样")
    void testProbabilityHalf() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.PROBABILITY);
        sampling.setProbability(0.5);

        TracingSampler sampler = new TracingSampler(sampling);

        int sampledCount = 0;
        int totalRuns = 1000;

        for (int i = 0; i < totalRuns; i++) {
            if (sampler.shouldSample()) {
                sampledCount++;
            }
        }

        // 允许 10% 的误差范围
        assertThat(sampledCount).isBetween((int) (totalRuns * 0.4), (int) (totalRuns * 0.6));
    }

    @Test
    @DisplayName("限流采样限制每秒请求数")
    void testRateLimiting() throws InterruptedException {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.RATE_LIMITING);
        sampling.setRate(10);

        TracingSampler sampler = new TracingSampler(sampling);

        // 快速发送大量请求
        AtomicInteger sampledCount = new AtomicInteger(0);
        for (int i = 0; i < 100; i++) {
            if (sampler.shouldSample()) {
                sampledCount.incrementAndGet();
            }
        }

        // 第一秒内应该不超过 10 个
        assertThat(sampledCount.get()).isLessThanOrEqualTo(10);

        // 等待窗口重置
        TimeUnit.SECONDS.sleep(1);

        // 新窗口应该允许新请求
        sampledCount.set(0);
        for (int i = 0; i < 20; i++) {
            if (sampler.shouldSample()) {
                sampledCount.incrementAndGet();
            }
        }

        assertThat(sampledCount.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("获取采样策略")
    void testGetStrategy() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.PROBABILITY);

        TracingSampler sampler = new TracingSampler(sampling);

        assertThat(sampler.getStrategy()).isEqualTo(SamplingStrategy.PROBABILITY);
    }

    @Test
    @DisplayName("获取概率配置")
    void testGetProbability() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.PROBABILITY);
        sampling.setProbability(0.8);

        TracingSampler sampler = new TracingSampler(sampling);

        assertThat(sampler.getProbability()).isEqualTo(0.8);
    }

    @Test
    @DisplayName("获取限流配置")
    void testGetRateLimit() {
        Sampling sampling = new Sampling();
        sampling.setStrategy(SamplingStrategy.RATE_LIMITING);
        sampling.setRate(50);

        TracingSampler sampler = new TracingSampler(sampling);

        assertThat(sampler.getRateLimit()).isEqualTo(50);
    }
}