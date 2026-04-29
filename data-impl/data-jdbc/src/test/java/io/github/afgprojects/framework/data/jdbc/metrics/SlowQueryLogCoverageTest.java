package io.github.afgprojects.framework.data.jdbc.metrics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SlowQueryLog 覆盖率补充测试
 */
@DisplayName("SlowQueryLog 覆盖率补充测试")
class SlowQueryLogCoverageTest {

    @Nested
    @DisplayName("formatParams 测试")
    class FormatParamsTests {

        @Test
        @DisplayName("null 参数应返回空括号")
        void shouldReturnEmptyBracketsForNullParams() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT 1",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                null,
                null
            );

            // When
            String result = log.formatParams();

            // Then
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("空数组应返回空括号")
        void shouldReturnEmptyBracketsForEmptyArray() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT 1",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                new Object[]{},
                null
            );

            // When
            String result = log.formatParams();

            // Then
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("包含 null 元素的参数")
        void shouldFormatParamsWithNullElement() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT * FROM test WHERE id = ?",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                new Object[]{null},
                null
            );

            // When
            String result = log.formatParams();

            // Then
            assertThat(result).isEqualTo("[null]");
        }

        @Test
        @DisplayName("混合类型参数")
        void shouldFormatMixedTypeParams() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT * FROM test WHERE id = ? AND name = ? AND active = ?",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                new Object[]{1L, "test", true},
                "Test"
            );

            // When
            String result = log.formatParams();

            // Then
            assertThat(result).isEqualTo("[1, 'test', true]");
        }
    }

    @Nested
    @DisplayName("toString 测试")
    class ToStringTests {

        @Test
        @DisplayName("基本 toString")
        void shouldFormatBasicToString() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT * FROM users",
                SqlOperationType.SELECT,
                Duration.ofMillis(500),
                null,
                null
            );

            // When
            String result = log.toString();

            // Then
            assertThat(result).contains("SlowQueryLog");
            assertThat(result).contains("type=SELECT");
            assertThat(result).contains("duration=500ms");
            assertThat(result).contains("sql=SELECT * FROM users");
            assertThat(result).doesNotContain("entity=");
            assertThat(result).doesNotContain("params=");
        }

        @Test
        @DisplayName("完整 toString 包含所有字段")
        void shouldFormatFullToString() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT * FROM users WHERE id = ?",
                SqlOperationType.SELECT,
                Duration.ofMillis(500),
                new Object[]{1L},
                "User"
            );

            // When
            String result = log.toString();

            // Then
            assertThat(result).contains("entity=User");
            assertThat(result).contains("params=[1]");
        }

        @Test
        @DisplayName("错误日志 toString")
        void shouldFormatErrorToString() {
            // Given
            SlowQueryLog log = SlowQueryLog.ofError(
                "SELECT * FROM users",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                null,
                null,
                "Connection timeout"
            );

            // When
            String result = log.toString();

            // Then
            assertThat(result).contains("error=Connection timeout");
        }
    }

    @Nested
    @DisplayName("isError 测试")
    class IsErrorTests {

        @Test
        @DisplayName("正常日志 isError 应返回 false")
        void normalLogShouldNotBeError() {
            // Given
            SlowQueryLog log = SlowQueryLog.of(
                "SELECT 1",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                null,
                null
            );

            // When & Then
            assertThat(log.isError()).isFalse();
        }

        @Test
        @DisplayName("错误日志 isError 应返回 true")
        void errorLogShouldBeError() {
            // Given
            SlowQueryLog log = SlowQueryLog.ofError(
                "SELECT 1",
                SqlOperationType.SELECT,
                Duration.ofMillis(100),
                null,
                null,
                "Some error"
            );

            // When & Then
            assertThat(log.isError()).isTrue();
        }
    }

    @Nested
    @DisplayName("record 访问器测试")
    class RecordAccessorTests {

        @Test
        @DisplayName("所有访问器应正确工作")
        void allAccessorsShouldWork() {
            // Given
            Object[] params = new Object[]{1L, "test"};
            SlowQueryLog log = new SlowQueryLog(
                "SELECT * FROM test",
                SqlOperationType.SELECT,
                Duration.ofMillis(200),
                params,
                "TestEntity",
                null
            );

            // When & Then
            assertThat(log.sql()).isEqualTo("SELECT * FROM test");
            assertThat(log.operationType()).isEqualTo(SqlOperationType.SELECT);
            assertThat(log.duration()).isEqualTo(Duration.ofMillis(200));
            assertThat(log.params()).isSameAs(params);
            assertThat(log.entityName()).isEqualTo("TestEntity");
            assertThat(log.errorMessage()).isNull();
        }
    }
}
