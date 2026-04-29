package io.github.afgprojects.framework.core.trace;

import java.util.concurrent.ThreadLocalRandom;

import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.trace.TracingProperties.Sampling;

/**
 * 追踪采样器
 * <p>
 * 支持多种采样策略，控制追踪数据的采集量。
 * </p>
 *
 * <h3>采样策略</h3>
 * <ul>
 *   <li>{@link SamplingStrategy#ALWAYS} - 全部采样</li>
 *   <li>{@link SamplingStrategy#NEVER} - 不采样</li>
 *   <li>{@link SamplingStrategy#PROBABILITY} - 概率采样</li>
 *   <li>{@link SamplingStrategy#RATE_LIMITING} - 限流采样</li>
 * </ul>
 */
public class TracingSampler {

    private final SamplingStrategy strategy;
    private final double probability;
    private final int rateLimit;
    private final long windowSizeNanos;

    // 限流采样状态
    private volatile long windowStartNanos;
    private final AtomicInteger atomicCounter = new AtomicInteger();

    /**
     * 创建采样器
     *
     * @param sampling 采样配置
     */
    public TracingSampler(Sampling sampling) {
        this.strategy = sampling.getStrategy();
        this.probability = sampling.getProbability();
        this.rateLimit = sampling.getRate();
        this.windowSizeNanos = 1_000_000_000L; // 1 秒窗口
        this.windowStartNanos = System.nanoTime();
    }

    /**
     * 判断当前请求是否应该被采样
     *
     * @return true 表示采样，false 表示不采样
     */
    public boolean shouldSample() {
        return switch (strategy) {
            case ALWAYS -> true;
            case NEVER -> false;
            case PROBABILITY -> probabilitySample();
            case RATE_LIMITING -> rateLimitSample();
        };
    }

    /**
     * 概率采样
     */
    private boolean probabilitySample() {
        if (probability >= 1.0) {
            return true;
        }
        if (probability <= 0.0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    /**
     * 限流采样
     */
    private boolean rateLimitSample() {
        long currentTimeNanos = System.nanoTime();

        // 检查是否需要重置窗口
        if (currentTimeNanos - windowStartNanos >= windowSizeNanos) {
            synchronized (this) {
                // 双重检查
                if (currentTimeNanos - windowStartNanos >= windowSizeNanos) {
                    windowStartNanos = currentTimeNanos;
                    atomicCounter.set(0);
                }
            }
        }

        // 原子递增并检查是否超过限制
        int count = atomicCounter.incrementAndGet();
        return count <= rateLimit;
    }

    /**
     * 获取当前采样策略
     *
     * @return 采样策略
     */
    public SamplingStrategy getStrategy() {
        return strategy;
    }

    /**
     * 获取概率采样配置
     *
     * @return 概率值（0.0 - 1.0）
     */
    public double getProbability() {
        return probability;
    }

    /**
     * 获取限流采样配置
     *
     * @return 每秒最大请求数
     */
    public int getRateLimit() {
        return rateLimit;
    }

    /**
     * 简单的原子整数计数器
     * <p>
     * 用于兼容 Java 25，避免 java.util.concurrent.atomic.AtomicInteger 的额外依赖
     * </p>
     */
    private static class AtomicInteger {
        private volatile int value;

        AtomicInteger() {
            this.value = 0;
        }

        int incrementAndGet() {
            synchronized (this) {
                return ++value;
            }
        }

        void set(int newValue) {
            synchronized (this) {
                value = newValue;
            }
        }
    }
}