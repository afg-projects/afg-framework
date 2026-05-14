package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link ListResult} 文件列表结果测试
 *
 * <p>测试文件列表结果的创建和属性：
 * <ul>
 *   <li>空结果</li>
 *   <li>对象列表</li>
 *   <li>公共前缀</li>
 *   <li>分页标记</li>
 * </ul>
 *
 * @see ListResult
 */
@DisplayName("ListResult 测试")
class ListResultTest {

    /**
     * 静态工厂方法测试
     */
    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        /**
         * 测试创建空结果
         */
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

        /**
         * 测试创建对象列表结果
         */
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

        /**
         * 测试创建带公共前缀的结果
         */
        @Test
        @DisplayName("应该创建带公共前缀的结果")
        void shouldCreateResultWithCommonPrefixes() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result = ListResult.of(List.of(obj), List.of("prefix1/", "prefix2/"));

            assertThat(result.objects()).hasSize(1);
            assertThat(result.commonPrefixes()).containsExactly("prefix1/", "prefix2/");
            assertThat(result.isTruncated()).isFalse();
        }

        /**
         * 测试创建带分页的结果
         */
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

    /**
     * 辅助方法测试
     */
    @Nested
    @DisplayName("辅助方法测试")
    class HelperMethodTests {

        /**
         * 测试 isEmpty 判断
         */
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

        /**
         * 测试 size 返回对象数量
         */
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

    /**
     * Record 特性测试
     */
    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        /**
         * 测试 equals 实现
         */
        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");
            ListResult result1 = ListResult.of(List.of(obj));
            ListResult result2 = ListResult.of(List.of(obj));

            assertThat(result1).isEqualTo(result2);
        }

        /**
         * 测试 hashCode 实现
         */
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