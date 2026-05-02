package io.github.afgprojects.framework.core.web.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import io.github.afgprojects.framework.core.support.BaseUnitTest;

/**
 * LivenessHealthIndicator 单元测试
 */
@DisplayName("LivenessHealthIndicator 测试")
class LivenessHealthIndicatorTest extends BaseUnitTest {

    private HealthCheckProperties properties;
    private LivenessHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        properties = new HealthCheckProperties();
        healthIndicator = new LivenessHealthIndicator(properties);
    }

    @Nested
    @DisplayName("health 测试")
    class HealthTest {

        @Test
        @DisplayName("正常情况应该返回 UP 状态")
        void shouldReturnUpWhenHealthy() {
            // when
            Health health = healthIndicator.health();

            // then
            assertThat(health.getStatus()).isEqualTo(Status.UP);
        }

        @Test
        @DisplayName("应该包含内存检查详情")
        void shouldContainMemoryDetails() {
            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details).containsKeys("heapUsed", "heapMax", "heapUsagePercent", "memoryStatus");
        }

        @Test
        @DisplayName("应该包含死锁检查详情")
        void shouldContainDeadlockDetails() {
            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details).containsKeys("deadlockDetected", "deadlockedThreadCount");
        }

        @Test
        @DisplayName("正常情况下应该没有死锁")
        void shouldNotHaveDeadlockNormally() {
            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details.get("deadlockDetected")).isEqualTo(false);
            assertThat(details.get("deadlockedThreadCount")).isEqualTo(0);
        }

        @Test
        @DisplayName("内存状态应该是 NORMAL 或 WARNING")
        void memoryStatusShouldBeNormalOrWarning() {
            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            String memoryStatus = (String) details.get("memoryStatus");
            assertThat(memoryStatus).isIn("NORMAL", "WARNING");
        }
    }

    @Nested
    @DisplayName("内存检查测试")
    class MemoryCheckTest {

        @Test
        @DisplayName("禁用内存检查时不应该包含内存详情")
        void shouldNotContainMemoryDetailsWhenDisabled() {
            // given
            properties.getLiveness().setMemoryCheckEnabled(false);

            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details).doesNotContainKeys("heapUsed", "memoryStatus");
        }

        @Test
        @DisplayName("应该包含非堆内存使用信息")
        void shouldContainNonHeapMemoryInfo() {
            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details).containsKey("nonHeapUsed");
        }
    }

    @Nested
    @DisplayName("死锁检查测试")
    class DeadlockCheckTest {

        @Test
        @DisplayName("禁用死锁检查时不应该包含死锁详情")
        void shouldNotContainDeadlockDetailsWhenDisabled() {
            // given
            properties.getLiveness().setDeadlockDetectionEnabled(false);

            // when
            Health health = healthIndicator.health();

            // then
            Map<String, Object> details = health.getDetails();
            assertThat(details).doesNotContainKeys("deadlockDetected", "deadlockedThreadCount");
        }
    }

    @Nested
    @DisplayName("配置测试")
    class ConfigurationTest {

        @Test
        @DisplayName("应该接受自定义阈值配置")
        void shouldAcceptCustomThresholds() {
            // given - 设置较高的阈值以适应当前测试环境
            properties.getLiveness().setMemoryWarningThreshold(90);
            properties.getLiveness().setMemoryCriticalThreshold(95);

            // when
            Health health = healthIndicator.health();

            // then - 验证健康检查能够正常执行
            assertThat(health.getStatus()).isNotNull();
        }
    }
}
