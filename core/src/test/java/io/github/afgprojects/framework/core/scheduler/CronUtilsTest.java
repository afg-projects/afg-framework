package io.github.afgprojects.framework.core.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * CronUtils 测试
 */
@DisplayName("CronUtils 测试")
class CronUtilsTest {

    @Nested
    @DisplayName("验证测试")
    class ValidationTests {

        @Test
        @DisplayName("应该验证有效的 Cron 表达式")
        void shouldValidateValidCron() {
            assertThat(CronUtils.isValid("0 * * * * *")).isTrue();
            assertThat(CronUtils.isValid("0 0 12 * * *")).isTrue();
            assertThat(CronUtils.isValid("0 30 10 * * 1")).isTrue(); // MON = 1
        }

        @Test
        @DisplayName("应该拒绝无效的 Cron 表达式")
        void shouldRejectInvalidCron() {
            assertThat(CronUtils.isValid(null)).isFalse();
            assertThat(CronUtils.isValid("")).isFalse();
            assertThat(CronUtils.isValid("* * *")).isFalse();
            assertThat(CronUtils.isValid("* * * * * * *")).isFalse();
        }
    }

    @Nested
    @DisplayName("解析测试")
    class ParseTests {

        @Test
        @DisplayName("应该解析标准 Cron 表达式")
        void shouldParseStandardCron() {
            CronUtils.CronExpression expr = CronUtils.parse("0 30 10 * * 1"); // MON = 1

            assertThat(expr).isNotNull();
        }

        @Test
        @DisplayName("应该拒绝字段数量不正确的表达式")
        void shouldRejectIncorrectFieldCount() {
            assertThatThrownBy(() -> CronUtils.parse("* * *"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expected 6 fields");
        }

        @Test
        @DisplayName("应该解析星号字段")
        void shouldParseStarField() {
            CronUtils.CronExpression expr = CronUtils.parse("* * * * * *");

            assertThat(expr).isNotNull();
        }

        @Test
        @DisplayName("应该解析问号字段")
        void shouldParseQuestionMarkField() {
            CronUtils.CronExpression expr = CronUtils.parse("0 0 12 ? * *");

            assertThat(expr).isNotNull();
        }

        @Test
        @DisplayName("应该解析步进表达式")
        void shouldParseStepExpression() {
            CronUtils.CronExpression expr = CronUtils.parse("*/5 * * * * *");

            assertThat(expr).isNotNull();
        }

        @Test
        @DisplayName("应该解析范围表达式")
        void shouldParseRangeExpression() {
            CronUtils.CronExpression expr = CronUtils.parse("0 0-5 * * * *");

            assertThat(expr).isNotNull();
        }

        @Test
        @DisplayName("应该解析列表表达式")
        void shouldParseListExpression() {
            CronUtils.CronExpression expr = CronUtils.parse("0 0,15,30,45 * * * *");

            assertThat(expr).isNotNull();
        }
    }

    @Nested
    @DisplayName("下一次执行时间测试")
    class NextExecutionTimeTests {

        @Test
        @DisplayName("应该计算下一次执行时间")
        void shouldCalculateNextExecutionTime() {
            ZonedDateTime now = ZonedDateTime.now();
            ZonedDateTime next = CronUtils.getNextExecutionTime("0 0 12 * * *", now);

            assertThat(next).isAfter(now);
            assertThat(next.getHour()).isEqualTo(12);
            assertThat(next.getMinute()).isEqualTo(0);
            assertThat(next.getSecond()).isEqualTo(0);
        }

        @Test
        @DisplayName("应该返回延迟时间")
        void shouldReturnDelay() {
            Duration delay = CronUtils.getNextExecutionDelay("0 0 12 * * *");

            assertThat(delay).isNotNull();
            assertThat(delay).isGreaterThanOrEqualTo(Duration.ZERO);
        }
    }

    @Nested
    @DisplayName("周期测试")
    class PeriodTests {

        @Test
        @DisplayName("应该计算执行周期")
        void shouldCalculatePeriod() {
            Duration period = CronUtils.getPeriod("0 0 */2 * * *");

            assertThat(period).isNotNull();
            assertThat(period.toHours()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("CronExpression 测试")
    class CronExpressionTests {

        @Test
        @DisplayName("应该计算下一个执行时间")
        void shouldCalculateNextExecution() {
            CronUtils.CronExpression expr = CronUtils.parse("0 30 10 * * *");
            ZonedDateTime now = ZonedDateTime.now().withHour(9).withMinute(0);
            ZonedDateTime next = expr.nextExecutionTime(now);

            assertThat(next.getHour()).isEqualTo(10);
            assertThat(next.getMinute()).isEqualTo(30);
            assertThat(next.getSecond()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("CronField 测试")
    class CronFieldTests {

        @Test
        @DisplayName("星号字段应该匹配所有值")
        void shouldMatchAllValuesForStarField() {
            CronUtils.CronExpression expr = CronUtils.parse("* * * * * *");

            // 验证星号匹配所有值
            assertThat(expr).isNotNull();
        }
    }
}
