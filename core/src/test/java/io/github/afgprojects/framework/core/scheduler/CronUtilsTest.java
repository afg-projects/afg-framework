package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * CronUtils 测试
 */
@DisplayName("CronUtils Tests")
class CronUtilsTest {

    @Test
    @DisplayName("Should validate correct cron expressions")
    void shouldValidateCorrectCron() {
        assertThat(CronUtils.isValid("0 * * * * ?")).isTrue();
        assertThat(CronUtils.isValid("0 0/5 * * * ?")).isTrue();
        assertThat(CronUtils.isValid("0 0 12 * * ?")).isTrue();
        assertThat(CronUtils.isValid("0 0 0 1 1 ?")).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid cron expressions")
    void shouldRejectInvalidCron() {
        assertThat(CronUtils.isValid(null)).isFalse();
        assertThat(CronUtils.isValid("")).isFalse();
        assertThat(CronUtils.isValid("0 0 12")).isFalse(); // 缺少字段
        assertThat(CronUtils.isValid("invalid")).isFalse();
    }

    @Test
    @DisplayName("Should parse simple cron expression")
    void shouldParseSimpleCron() {
        CronUtils.CronExpression expr = CronUtils.parse("0 * * * * ?");

        assertThat(expr).isNotNull();
    }

    @Test
    @DisplayName("Should throw exception for invalid cron")
    void shouldThrowForInvalidCron() {
        assertThatThrownBy(() -> CronUtils.parse("invalid"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should calculate next execution time for every minute")
    void shouldCalculateNextExecutionForEveryMinute() {
        String cron = "0 * * * * ?";

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = CronUtils.getNextExecutionTime(cron, now);

        assertThat(next).isAfter(now);
        assertThat(next.getSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should calculate next execution delay")
    void shouldCalculateNextExecutionDelay() {
        String cron = "0 0/5 * * * ?"; // 每 5 分钟

        Duration delay = CronUtils.getNextExecutionDelay(cron);

        assertThat(delay).isNotNull();
        assertThat(delay.toMillis()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should calculate period for fixed interval cron")
    void shouldCalculatePeriod() {
        String cron = "0 0/5 * * * ?"; // 每 5 分钟

        Duration period = CronUtils.getPeriod(cron);

        assertThat(period).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Should handle step expressions")
    void shouldHandleStepExpressions() {
        CronUtils.CronExpression expr = CronUtils.parse("0 */5 * * * ?");

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = expr.nextExecutionTime(now);

        assertThat(next).isAfter(now);
        assertThat(next.getMinute() % 5).isEqualTo(0);
        assertThat(next.getSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle range expressions")
    void shouldHandleRangeExpressions() {
        CronUtils.CronExpression expr = CronUtils.parse("0 0 9-17 * * 1-5");

        ZonedDateTime next = expr.nextExecutionTime(ZonedDateTime.now());

        assertThat(next).isNotNull();
    }

    @Test
    @DisplayName("Should handle specific values")
    void shouldHandleSpecificValues() {
        CronUtils.CronExpression expr = CronUtils.parse("30 15 10 * * ?");

        ZonedDateTime next = expr.nextExecutionTime(ZonedDateTime.now());

        assertThat(next.getSecond()).isEqualTo(30);
        assertThat(next.getMinute()).isEqualTo(15);
        assertThat(next.getHour()).isEqualTo(10);
    }

    @Test
    @DisplayName("Should handle wildcard for all values")
    void shouldHandleWildcard() {
        CronUtils.CronExpression expr = CronUtils.parse("0 * * * * ?");

        // 验证下一个执行时间的秒数是否为 0
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = expr.nextExecutionTime(now);

        assertThat(next.getSecond()).isEqualTo(0);
    }
}
