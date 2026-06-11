package io.github.afgprojects.framework.commons.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DateUtils 测试")
class DateUtilsTest {

    @Nested
    @DisplayName("format() 方法")
    class FormatTests {

        @Test
        @DisplayName("应正确格式化 Instant")
        void shouldFormatInstant() {
            Instant instant = Instant.parse("2026-06-11T10:30:00Z");
            assertThat(DateUtils.format(instant, "yyyy-MM-dd")).isEqualTo("2026-06-11");
        }

        @Test
        @DisplayName("Instant 为 null 时应返回 null")
        void shouldReturnNullWhenInstantIsNull() {
            Instant instant = null;
            assertThat(DateUtils.format(instant, "yyyy-MM-dd")).isNull();
        }

        @Test
        @DisplayName("应正确格式化 LocalDateTime")
        void shouldFormatLocalDateTime() {
            LocalDateTime dateTime = LocalDateTime.of(2026, 6, 11, 10, 30);
            assertThat(DateUtils.format(dateTime, "yyyy-MM-dd HH:mm")).isEqualTo("2026-06-11 10:30");
        }

        @Test
        @DisplayName("LocalDateTime 为 null 时应返回 null")
        void shouldReturnNullWhenLocalDateTimeIsNull() {
            LocalDateTime dateTime = null;
            assertThat(DateUtils.format(dateTime, "yyyy-MM-dd")).isNull();
        }
    }

    @Nested
    @DisplayName("parse() 方法")
    class ParseTests {

        @Test
        @DisplayName("应正确解析 LocalDate")
        void shouldParseLocalDate() {
            LocalDate date = DateUtils.parseLocalDate("2026-06-11", "yyyy-MM-dd");
            assertThat(date).isEqualTo(LocalDate.of(2026, 6, 11));
        }

        @Test
        @DisplayName("字符串为 null 时应返回 null")
        void shouldReturnNullWhenTextIsNull() {
            assertThat(DateUtils.parseLocalDate(null, "yyyy-MM-dd")).isNull();
        }

        @Test
        @DisplayName("应正确解析 LocalDateTime")
        void shouldParseLocalDateTime() {
            LocalDateTime dateTime = DateUtils.parseLocalDateTime("2026-06-11 10:30", "yyyy-MM-dd HH:mm");
            assertThat(dateTime).isEqualTo(LocalDateTime.of(2026, 6, 11, 10, 30));
        }
    }

    @Nested
    @DisplayName("between() 方法")
    class BetweenTests {

        @Test
        @DisplayName("时间在范围内时应返回 true")
        void shouldReturnTrueWhenTimeIsBetween() {
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            Instant end = Instant.parse("2026-12-31T23:59:59Z");
            Instant value = Instant.parse("2026-06-11T10:30:00Z");

            assertThat(DateUtils.between(start, end, value)).isTrue();
        }

        @Test
        @DisplayName("时间等于边界时应返回 true")
        void shouldReturnTrueWhenTimeEqualsBoundary() {
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            Instant end = Instant.parse("2026-12-31T23:59:59Z");

            assertThat(DateUtils.between(start, end, start)).isTrue();
            assertThat(DateUtils.between(start, end, end)).isTrue();
        }

        @Test
        @DisplayName("时间超出范围时应返回 false")
        void shouldReturnFalseWhenTimeIsOutside() {
            Instant start = Instant.parse("2026-01-01T00:00:00Z");
            Instant end = Instant.parse("2026-12-31T23:59:59Z");
            Instant before = Instant.parse("2025-12-31T23:59:59Z");
            Instant after = Instant.parse("2027-01-01T00:00:00Z");

            assertThat(DateUtils.between(start, end, before)).isFalse();
            assertThat(DateUtils.between(start, end, after)).isFalse();
        }

        @Test
        @DisplayName("任一参数为 null 时应返回 false")
        void shouldReturnFalseWhenAnyArgIsNull() {
            assertThat(DateUtils.between(null, Instant.now(), Instant.now())).isFalse();
            assertThat(DateUtils.between(Instant.now(), null, Instant.now())).isFalse();
            assertThat(DateUtils.between(Instant.now(), Instant.now(), null)).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired() 方法")
    class IsExpiredTests {

        @Test
        @DisplayName("过去的时间应返回 true")
        void shouldReturnTrueForPastTime() {
            assertThat(DateUtils.isExpired(Instant.now().minusSeconds(60))).isTrue();
        }

        @Test
        @DisplayName("未来的时间应返回 false")
        void shouldReturnFalseForFutureTime() {
            assertThat(DateUtils.isExpired(Instant.now().plusSeconds(3600))).isFalse();
        }

        @Test
        @DisplayName("null 应返回 true")
        void shouldReturnTrueWhenNull() {
            assertThat(DateUtils.isExpired(null)).isTrue();
        }

        @Test
        @DisplayName("指定参考时间的过期判断")
        void shouldCheckExpiryAgainstReference() {
            Instant past = Instant.parse("2026-01-01T00:00:00Z");
            Instant reference = Instant.parse("2026-06-11T00:00:00Z");

            assertThat(DateUtils.isExpired(past, reference)).isTrue();
            assertThat(DateUtils.isExpired(reference.plusSeconds(1), reference)).isFalse();
        }
    }
}