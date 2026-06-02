package io.github.afgprojects.framework.ai.observability;

import io.github.afgprojects.framework.ai.core.api.observability.MetricsCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DefaultMetricsCollector 单元测试
 */
class DefaultMetricsCollectorTest {

    private DefaultMetricsCollector collector;

    @BeforeEach
    void setUp() {
        collector = new DefaultMetricsCollector();
    }

    @Test
    @DisplayName("记录请求计数")
    void recordCount() {
        collector.recordCount("chat", "gpt-4", "success", Map.of());
        collector.recordCount("chat", "gpt-4", "success", Map.of());
        collector.recordCount("chat", "gpt-4", "failure", Map.of());

        var summary = collector.getSummary();

        assertThat(summary.getTotalRequests()).isEqualTo(3);
        assertThat(summary.getSuccessRequests()).isEqualTo(2);
        assertThat(summary.getFailedRequests()).isEqualTo(1);
    }

    @Test
    @DisplayName("记录 Token 使用量")
    void recordTokenUsage() {
        collector.recordTokenUsage("gpt-4", 100, 50, Map.of());
        collector.recordTokenUsage("gpt-4", 200, 100, Map.of());

        var summary = collector.getSummary();

        assertThat(summary.getTotalTokens()).isEqualTo(450);
    }

    @Test
    @DisplayName("记录成本")
    void recordCost() {
        collector.recordCost("gpt-4", 0.01, Map.of());
        collector.recordCost("gpt-4", 0.02, Map.of());

        var summary = collector.getSummary();

        assertThat(summary.getTotalCost()).isEqualTo(0.03);
    }

    @Test
    @DisplayName("计时器记录响应时间")
    void timer_recordsResponseTime() throws Exception {
        MetricsCollector.Timer timer = collector.startTimer("chat", "gpt-4", Map.of());

        // 等待一段时间
        Thread.sleep(100);

        timer.stop("success");

        var summary = collector.getSummary();

        assertThat(summary.getAverageResponseTime()).isGreaterThanOrEqualTo(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("获取模型统计")
    void getModelStats() {
        collector.recordCount("chat", "gpt-4", "success", Map.of());
        collector.recordTokenUsage("gpt-4", 100, 50, Map.of());
        collector.recordCost("gpt-4", 0.01, Map.of());

        collector.recordCount("chat", "gpt-3.5", "success", Map.of());
        collector.recordTokenUsage("gpt-3.5", 50, 25, Map.of());

        var summary = collector.getSummary();
        var modelStats = summary.getModelStats();

        assertThat(modelStats).containsKeys("gpt-4", "gpt-3.5");

        var gpt4Stats = modelStats.get("gpt-4");
        assertThat(gpt4Stats.getRequestCount()).isEqualTo(1);
        assertThat(gpt4Stats.getTotalInputTokens()).isEqualTo(100);
        assertThat(gpt4Stats.getTotalOutputTokens()).isEqualTo(50);
        assertThat(gpt4Stats.getTotalCost()).isEqualTo(0.01);
    }

    @Test
    @DisplayName("计时器获取已耗时")
    void timer_getElapsed() throws InterruptedException {
        MetricsCollector.Timer timer = collector.startTimer("chat", "gpt-4", Map.of());

        Thread.sleep(50);

        Duration elapsed = timer.getElapsed();

        assertThat(elapsed).isGreaterThanOrEqualTo(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("计时器获取开始时间戳")
    void timer_getStartTimeMs() {
        long before = System.currentTimeMillis();
        MetricsCollector.Timer timer = collector.startTimer("chat", "gpt-4", Map.of());
        long after = System.currentTimeMillis();

        assertThat(timer.getStartTimeMs()).isBetween(before, after);
    }
}