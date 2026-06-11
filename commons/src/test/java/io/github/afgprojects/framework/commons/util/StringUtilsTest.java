package io.github.afgprojects.framework.commons.util;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StringUtils 测试")
class StringUtilsTest {

    @Nested
    @DisplayName("isBlank() 方法")
    class IsBlankTests {

        @Test
        @DisplayName("null 应返回 true")
        void shouldReturnTrueForNull() {
            assertThat(StringUtils.isBlank(null)).isTrue();
        }

        @Test
        @DisplayName("空字符串应返回 true")
        void shouldReturnTrueForEmpty() {
            assertThat(StringUtils.isBlank("")).isTrue();
        }

        @Test
        @DisplayName("纯空白应返回 true")
        void shouldReturnTrueForWhitespace() {
            assertThat(StringUtils.isBlank("   ")).isTrue();
        }

        @Test
        @DisplayName("有内容应返回 false")
        void shouldReturnFalseForContent() {
            assertThat(StringUtils.isBlank("hello")).isFalse();
        }
    }

    @Nested
    @DisplayName("isNotBlank() 方法")
    class IsNotBlankTests {

        @Test
        @DisplayName("null 应返回 false")
        void shouldReturnFalseForNull() {
            assertThat(StringUtils.isNotBlank(null)).isFalse();
        }

        @Test
        @DisplayName("有内容应返回 true")
        void shouldReturnTrueForContent() {
            assertThat(StringUtils.isNotBlank("hello")).isTrue();
        }
    }

    @Nested
    @DisplayName("truncate() 方法")
    class TruncateTests {

        @Test
        @DisplayName("超出长度时应截断并加省略号")
        void shouldTruncateWithEllipsis() {
            assertThat(StringUtils.truncate("hello world", 5)).isEqualTo("hello...");
        }

        @Test
        @DisplayName("未超出长度时应返回原字符串")
        void shouldReturnOriginalWhenShort() {
            assertThat(StringUtils.truncate("hello", 10)).isEqualTo("hello");
        }

        @Test
        @DisplayName("null 应返回 null")
        void shouldReturnNullForNull() {
            assertThat(StringUtils.truncate(null, 5)).isNull();
        }

        @Test
        @DisplayName("空字符串应返回空字符串")
        void shouldReturnEmptyForEmpty() {
            assertThat(StringUtils.truncate("", 5)).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("join() 方法")
    class JoinTests {

        @Test
        @DisplayName("应正确拼接列表")
        void shouldJoinList() {
            assertThat(StringUtils.join(List.of("a", "b", "c"), ",")).isEqualTo("a,b,c");
        }

        @Test
        @DisplayName("null 列表应返回空字符串")
        void shouldReturnEmptyForNullList() {
            List<String> list = null;
            assertThat(StringUtils.join(list, ",")).isEqualTo("");
        }

        @Test
        @DisplayName("应正确拼接数组")
        void shouldJoinArray() {
            assertThat(StringUtils.join(new String[]{"x", "y"}, "-")).isEqualTo("x-y");
        }

        @Test
        @DisplayName("null 数组应返回空字符串")
        void shouldReturnEmptyForNullArray() {
            String[] arr = null;
            assertThat(StringUtils.join(arr, "-")).isEqualTo("");
        }
    }

    @Nested
    @DisplayName("splitAndTrim() 方法")
    class SplitAndTrimTests {

        @Test
        @DisplayName("应拆分并去除空白")
        void shouldSplitAndTrim() {
            assertThat(StringUtils.splitAndTrim("a, b, c", ",")).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("应过滤空元素")
        void shouldFilterEmptyElements() {
            assertThat(StringUtils.splitAndTrim("a,,b,,c", ",")).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("null 应返回空列表")
        void shouldReturnEmptyForNull() {
            assertThat(StringUtils.splitAndTrim(null, ",")).isEmpty();
        }

        @Test
        @DisplayName("空白字符串应返回空列表")
        void shouldReturnEmptyForBlank() {
            assertThat(StringUtils.splitAndTrim("   ", ",")).isEmpty();
        }
    }
}