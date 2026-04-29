package io.github.afgprojects.framework.core.metrics;

import io.github.afgprojects.framework.core.autoconfigure.MetricsProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CustomMetrics 测试
 */
@DisplayName("CustomMetrics 测试")
class CustomMetricsTest {

    private MeterRegistry meterRegistry;
    private MetricsProperties properties;
    private CustomMetrics customMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        properties = new MetricsProperties();
        customMetrics = new CustomMetrics(meterRegistry, properties);
    }

    @Test
    @DisplayName("应该创建简单计数器")
    void shouldCreateSimpleCounter() {
        Counter counter = customMetrics.counter("test.counter");
        assertNotNull(counter);
        counter.increment();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("应该创建带标签的计数器")
    void shouldCreateCounterWithTags() {
        Counter counter = customMetrics.counter("test.counter", "type", "test", "region", "cn");
        assertNotNull(counter);
        counter.increment(5);
        assertEquals(5.0, counter.count());
    }

    @Test
    @DisplayName("应该创建带描述和标签映射的计数器")
    void shouldCreateCounterWithDescriptionAndTags() {
        Counter counter = customMetrics.counter("test.counter", "Test description", Map.of("env", "dev"));
        assertNotNull(counter);
        counter.increment();
        assertEquals(1.0, counter.count());
    }

    @Test
    @DisplayName("应该创建简单 Timer")
    void shouldCreateSimpleTimer() {
        Timer timer = customMetrics.timer("test.timer");
        assertNotNull(timer);
        timer.record(Duration.ofMillis(100));
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("应该创建带标签的 Timer")
    void shouldCreateTimerWithTags() {
        Timer timer = customMetrics.timer("test.timer", "endpoint", "/api/users");
        assertNotNull(timer);
        timer.record(Duration.ofMillis(50));
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("应该正确启动和停止计时")
    void shouldStartAndStopTimer() {
        Timer.Sample sample = customMetrics.startTimer();
        assertNotNull(sample);
        // 模拟一些工作
        customMetrics.stopTimer(sample, "test.operation", "action", "test");
        Timer timer = meterRegistry.find("test.operation").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("应该注册 Gauge")
    void shouldRegisterGauge() {
        AtomicInteger value = new AtomicInteger(42);
        Gauge gauge = customMetrics.gauge("test.gauge", value, AtomicInteger::get);
        assertNotNull(gauge);
        assertEquals(42.0, gauge.value());
        value.set(100);
        assertEquals(100.0, gauge.value());
    }

    @Test
    @DisplayName("应该注册带标签的 Gauge")
    void shouldRegisterGaugeWithTags() {
        AtomicInteger value = new AtomicInteger(10);
        Gauge gauge = customMetrics.gauge("test.gauge", value, AtomicInteger::get, "type", "queue");
        assertNotNull(gauge);
        assertEquals(10.0, gauge.value());
    }

    @Test
    @DisplayName("应该增加计数器值")
    void shouldIncrementCounter() {
        customMetrics.increment("test.increment", 3.0);
        Counter counter = meterRegistry.find("test.increment").counter();
        assertNotNull(counter);
        assertEquals(3.0, counter.count());
    }

    @Test
    @DisplayName("应该增加带标签的计数器值")
    void shouldIncrementCounterWithTags() {
        customMetrics.increment("test.increment", 2.5, "status", "success");
        Counter counter = meterRegistry.find("test.increment").counter();
        assertNotNull(counter);
        assertEquals(2.5, counter.count());
    }

    @Test
    @DisplayName("应该记录执行时间")
    void shouldRecordDuration() {
        customMetrics.record("test.duration", Duration.ofMillis(200));
        Timer timer = meterRegistry.find("test.duration").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("应该记录 Runnable 执行时间")
    void shouldRecordRunnable() {
        AtomicInteger executed = new AtomicInteger(0);
        customMetrics.record("test.runnable", (Runnable) () -> executed.incrementAndGet());
        Timer timer = meterRegistry.find("test.runnable").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertEquals(1, executed.get());
    }

    @Test
    @DisplayName("应该记录 Callable 执行时间并返回结果")
    void shouldRecordCallable() throws Exception {
        String result = customMetrics.record("test.callable", () -> "success");
        assertEquals("success", result);
        Timer timer = meterRegistry.find("test.callable").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    @DisplayName("应该返回 MeterRegistry")
    void shouldReturnMeterRegistry() {
        assertEquals(meterRegistry, customMetrics.getMeterRegistry());
    }
}
