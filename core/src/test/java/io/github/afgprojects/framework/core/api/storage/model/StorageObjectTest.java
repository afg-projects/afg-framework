package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * StorageObject 测试
 */
@DisplayName("StorageObject 测试")
class StorageObjectTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建简化存储对象")
        void shouldCreateSimpleStorageObject() {
            StorageObject obj = StorageObject.of("test.txt", 100, "text/plain");

            assertThat(obj.key()).isEqualTo("test.txt");
            assertThat(obj.size()).isEqualTo(100);
            assertThat(obj.contentType()).isEqualTo("text/plain");
            assertThat(obj.lastModified()).isNotNull();
        }

        @Test
        @DisplayName("应该创建带元数据的存储对象")
        void shouldCreateStorageObjectWithMetadata() {
            StorageMetadata metadata = StorageMetadata.builder()
                    .put("author", "test")
                    .build();

            StorageObject obj = StorageObject.of("doc.pdf", 1000, "application/pdf", metadata);

            assertThat(obj.key()).isEqualTo("doc.pdf");
            assertThat(obj.size()).isEqualTo(1000);
            assertThat(obj.contentType()).isEqualTo("application/pdf");
            assertThat(obj.metadata()).isSameAs(metadata);
        }
    }

    @Nested
    @DisplayName("文件名提取测试")
    class FileNameExtractionTests {

        @Test
        @DisplayName("应该从简单路径提取文件名")
        void shouldExtractFileNameFromSimplePath() {
            StorageObject obj = new StorageObject("test.txt", 100, null, null, null, null);

            assertThat(obj.getFileName()).isEqualTo("test.txt");
        }

        @Test
        @DisplayName("应该从带路径的 key 提取文件名")
        void shouldExtractFileNameFromPath() {
            StorageObject obj = new StorageObject("images/2024/avatar.jpg", 1000, null, null, null, null);

            assertThat(obj.getFileName()).isEqualTo("avatar.jpg");
        }

        @Test
        @DisplayName("应该处理多层路径")
        void shouldHandleDeepPath() {
            StorageObject obj = new StorageObject("a/b/c/d/file.txt", 100, null, null, null, null);

            assertThat(obj.getFileName()).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("应该处理以斜杠结尾的路径")
        void shouldHandleTrailingSlash() {
            StorageObject obj = new StorageObject("path/to/dir/", 0, null, null, null, null);

            // 以斜杠结尾时，getFileName 返回空字符串
            assertThat(obj.getFileName()).isEmpty();
        }
    }

    @Nested
    @DisplayName("扩展名提取测试")
    class ExtensionExtractionTests {

        @Test
        @DisplayName("应该提取文件扩展名")
        void shouldExtractExtension() {
            StorageObject obj = new StorageObject("document.pdf", 100, null, null, null, null);

            assertThat(obj.getExtension()).isEqualTo("pdf");
        }

        @Test
        @DisplayName("应该从带路径的 key 提取扩展名")
        void shouldExtractExtensionFromPath() {
            StorageObject obj = new StorageObject("images/2024/avatar.jpg", 1000, null, null, null, null);

            assertThat(obj.getExtension()).isEqualTo("jpg");
        }

        @Test
        @DisplayName("应该对无扩展名的文件返回 null")
        void shouldReturnNullForNoExtension() {
            StorageObject obj = new StorageObject("README", 100, null, null, null, null);

            assertThat(obj.getExtension()).isNull();
        }

        @Test
        @DisplayName("应该处理多个点号")
        void shouldHandleMultipleDots() {
            StorageObject obj = new StorageObject("archive.tar.gz", 1000, null, null, null, null);

            assertThat(obj.getExtension()).isEqualTo("gz");
        }

        @Test
        @DisplayName("应该处理隐藏文件")
        void shouldHandleHiddenFile() {
            StorageObject obj = new StorageObject(".gitignore", 100, null, null, null, null);

            // .gitignore 的点号在位置 0，不满足 lastDot > 0，所以返回 null
            assertThat(obj.getExtension()).isNull();
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            Instant now = Instant.now();
            StorageObject obj1 = new StorageObject("test.txt", 100, "text/plain", "etag1", now, null);
            StorageObject obj2 = new StorageObject("test.txt", 100, "text/plain", "etag1", now, null);

            assertThat(obj1).isEqualTo(obj2);
        }

        @Test
        @DisplayName("应该正确实现 hashCode")
        void shouldImplementHashCode() {
            Instant now = Instant.now();
            StorageObject obj1 = new StorageObject("test.txt", 100, "text/plain", "etag1", now, null);
            StorageObject obj2 = new StorageObject("test.txt", 100, "text/plain", "etag1", now, null);

            assertThat(obj1.hashCode()).isEqualTo(obj2.hashCode());
        }
    }
}