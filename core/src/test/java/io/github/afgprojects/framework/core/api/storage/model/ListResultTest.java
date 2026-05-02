package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ListResult 测试
 */
@DisplayName("ListResult 测试")
class ListResultTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建空结果")
        void shouldCreateEmptyResult() {
            ListResult result = ListResult.empty();

            assertThat(result.objects()).isEmpty();
            assertThat(result.commonPrefixes()).isEmpty();
            assertThat(result.isTruncated()).isFalse();
            assertThat(result.nextMarker()).isNull();
            assertThat(result.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("应该创建对象列表结果")
        void shouldCreateObjectListResult() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result = ListResult.of(List.of(obj));

            assertThat(result.objects()).hasSize(1);
            assertThat(result.commonPrefixes()).isEmpty();
            assertThat(result.isTruncated()).isFalse();
            assertThat(result.nextMarker()).isNull();
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("应该创建带公共前缀的结果")
        void shouldCreateResultWithCommonPrefixes() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result = ListResult.of(List.of(obj), List.of("prefix1/", "prefix2/"));

            assertThat(result.objects()).hasSize(1);
            assertThat(result.commonPrefixes()).containsExactly("prefix1/", "prefix2/");
            assertThat(result.isTruncated()).isFalse();
        }

        @Test
        @DisplayName("应该创建带分页的结果")
        void shouldCreatePaginatedResult() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result = ListResult.of(
                    List.of(obj),
                    List.of("prefix/"),
                    true,
                    "next-marker"
            );

            assertThat(result.isTruncated()).isTrue();
            assertThat(result.nextMarker()).isEqualTo("next-marker");
        }
    }

    @Nested
    @DisplayName("辅助方法测试")
    class HelperMethodTests {

        @Test
        @DisplayName("isEmpty 应该正确判断")
        void shouldCheckIsEmpty() {
            ListResult empty = ListResult.empty();
            ListResult withObject = ListResult.of(List.of(
                    StorageObject.of("test.txt", 100, "text/plain")
            ));
            ListResult withPrefix = ListResult.of(
                    Collections.emptyList(),
                    List.of("prefix/")
            );

            assertThat(empty.isEmpty()).isTrue();
            assertThat(withObject.isEmpty()).isFalse();
            assertThat(withPrefix.isEmpty()).isFalse();
        }

        @Test
        @DisplayName("size 应该返回对象数量")
        void shouldReturnObjectCount() {
            ListResult result = ListResult.of(List.of(
                    StorageObject.of("file1.txt", 100, "text/plain"),
                    StorageObject.of("file2.txt", 200, "text/plain")
            ));

            assertThat(result.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result1 = ListResult.of(List.of(obj));
            ListResult result2 = ListResult.of(List.of(obj));

            assertThat(result1).isEqualTo(result2);
        }

        @Test
        @DisplayName("应该正确实现 hashCode")
        void shouldImplementHashCode() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result1 = ListResult.of(List.of(obj));
            ListResult result2 = ListResult.of(List.of(obj));

            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }
    }
}