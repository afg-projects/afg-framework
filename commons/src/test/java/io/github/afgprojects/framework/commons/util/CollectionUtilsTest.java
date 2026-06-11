package io.github.afgprojects.framework.commons.util;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CollectionUtils 测试")
class CollectionUtilsTest {

    @Nested
    @DisplayName("isEmpty() 方法")
    class IsEmptyTests {

        @Test
        @DisplayName("null 集合应返回 true")
        void shouldReturnTrueForNullCollection() {
            List<?> list = null;
            assertThat(CollectionUtils.isEmpty(list)).isTrue();
        }

        @Test
        @DisplayName("空集合应返回 true")
        void shouldReturnTrueForEmptyCollection() {
            assertThat(CollectionUtils.isEmpty(List.of())).isTrue();
        }

        @Test
        @DisplayName("非空集合应返回 false")
        void shouldReturnFalseForNonEmptyCollection() {
            assertThat(CollectionUtils.isEmpty(List.of("a"))).isFalse();
        }

        @Test
        @DisplayName("null Map 应返回 true")
        void shouldReturnTrueForNullMap() {
            Map<?, ?> map = null;
            assertThat(CollectionUtils.isEmpty(map)).isTrue();
        }

        @Test
        @DisplayName("空 Map 应返回 true")
        void shouldReturnTrueForEmptyMap() {
            assertThat(CollectionUtils.isEmpty(Map.of())).isTrue();
        }
    }

    @Nested
    @DisplayName("isNotEmpty() 方法")
    class IsNotEmptyTests {

        @Test
        @DisplayName("null 集合应返回 false")
        void shouldReturnFalseForNullCollection() {
            List<?> list = null;
            assertThat(CollectionUtils.isNotEmpty(list)).isFalse();
        }

        @Test
        @DisplayName("非空集合应返回 true")
        void shouldReturnTrueForNonEmptyCollection() {
            assertThat(CollectionUtils.isNotEmpty(List.of("a"))).isTrue();
        }
    }

    @Nested
    @DisplayName("first() 方法")
    class FirstTests {

        @Test
        @DisplayName("应返回列表第一个元素")
        void shouldReturnFirstElement() {
            assertThat(CollectionUtils.first(List.of("a", "b", "c"))).isEqualTo("a");
        }

        @Test
        @DisplayName("空列表应返回 null")
        void shouldReturnNullForEmptyList() {
            assertThat(CollectionUtils.<String>first(List.of())).isNull();
        }

        @Test
        @DisplayName("null 列表应返回 null")
        void shouldReturnNullForNullList() {
            List<String> list = null;
            assertThat(CollectionUtils.first(list)).isNull();
        }
    }

    @Nested
    @DisplayName("last() 方法")
    class LastTests {

        @Test
        @DisplayName("应返回列表最后一个元素")
        void shouldReturnLastElement() {
            assertThat(CollectionUtils.last(List.of("a", "b", "c"))).isEqualTo("c");
        }

        @Test
        @DisplayName("空列表应返回 null")
        void shouldReturnNullForEmptyList() {
            assertThat(CollectionUtils.<String>last(List.of())).isNull();
        }

        @Test
        @DisplayName("null 列表应返回 null")
        void shouldReturnNullForNullList() {
            List<String> list = null;
            assertThat(CollectionUtils.last(list)).isNull();
        }
    }

    @Nested
    @DisplayName("partition() 方法")
    class PartitionTests {

        @Test
        @DisplayName("应正确分区列表")
        void shouldPartitionList() {
            List<List<Integer>> result = CollectionUtils.partition(List.of(1, 2, 3, 4, 5), 2);

            assertThat(result).hasSize(3);
            assertThat(result.get(0)).containsExactly(1, 2);
            assertThat(result.get(1)).containsExactly(3, 4);
            assertThat(result.get(2)).containsExactly(5);
        }

        @Test
        @DisplayName("列表大小等于 batchSize 时应返回一个分区")
        void shouldReturnSinglePartitionWhenSizeEqualsBatchSize() {
            List<List<Integer>> result = CollectionUtils.partition(List.of(1, 2, 3), 3);

            assertThat(result).hasSize(1);
            assertThat(result.get(0)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("空列表应返回空分区列表")
        void shouldReturnEmptyForEmptyList() {
            assertThat(CollectionUtils.partition(List.of(), 3)).isEmpty();
        }

        @Test
        @DisplayName("null 列表应返回空分区列表")
        void shouldReturnEmptyForNullList() {
            assertThat(CollectionUtils.partition(null, 3)).isEmpty();
        }

        @Test
        @DisplayName("batchSize 为 0 或负数时应返回空分区列表")
        void shouldReturnEmptyForInvalidBatchSize() {
            assertThat(CollectionUtils.partition(List.of(1, 2, 3), 0)).isEmpty();
            assertThat(CollectionUtils.partition(List.of(1, 2, 3), -1)).isEmpty();
        }
    }
}