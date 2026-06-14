package io.github.afgprojects.framework.integration.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.afgprojects.framework.core.api.storage.FileStorage;
import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;
import io.github.afgprojects.framework.integration.storage.local.LocalFileStorage;
import io.github.afgprojects.framework.integration.storage.model.StorageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LocalFileStorage 集成测试
 *
 * <p>基于真实文件系统测试本地存储操作
 */
@DisplayName("LocalFileStorage 本地文件存储测试")
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private FileStorage fileStorage;

    @BeforeEach
    void setUp() {
        fileStorage = new LocalFileStorage("test-bucket", tempDir.toString(), "http://localhost:8080");
    }

    @Nested
    @DisplayName("基本属性")
    class BasicProperties {

        @Test
        @DisplayName("getStorageType 应返回 LOCAL")
        void shouldReturnLocalStorageType() {
            assertThat(fileStorage.getStorageType()).isEqualTo(StorageType.LOCAL);
        }

        @Test
        @DisplayName("getBucket 应返回配置的 bucket 名称")
        void shouldReturnBucketName() {
            assertThat(fileStorage.getBucket()).isEqualTo("test-bucket");
        }
    }

    @Nested
    @DisplayName("upload / download 操作")
    class UploadDownload {

        @Test
        @DisplayName("upload 后应能 download 相同内容")
        void shouldUploadAndDownload() throws IOException {
            byte[] content = "Hello, World!".getBytes();
            UploadRequest request = UploadRequest.of("test.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain");

            StorageObject uploaded = fileStorage.upload(request);
            assertThat(uploaded.key()).isEqualTo("test.txt");
            assertThat(uploaded.size()).isEqualTo(content.length);

            DownloadResult result = fileStorage.download("test.txt");
            assertThat(result.size()).isEqualTo(content.length);

            byte[] downloaded = result.inputStream().readAllBytes();
            assertThat(downloaded).isEqualTo(content);
        }

        @Test
        @DisplayName("upload 到子目录应正常工作")
        void shouldUploadToSubdirectory() throws IOException {
            byte[] content = "nested content".getBytes();
            UploadRequest request = UploadRequest.of("sub/dir/file.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain");

            StorageObject uploaded = fileStorage.upload(request);
            assertThat(uploaded.key()).isEqualTo("sub/dir/file.txt");

            DownloadResult result = fileStorage.download("sub/dir/file.txt");
            assertThat(result.size()).isEqualTo(content.length);
        }

        @Test
        @DisplayName("download 不存在的文件应抛出 StorageException")
        void shouldThrowException_whenDownloadNonExistent() {
            assertThatThrownBy(() -> fileStorage.download("nonexistent.txt"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("exists 操作")
    class Exists {

        @Test
        @DisplayName("exists 应正确判断文件是否存在")
        void shouldDetectFileExistence() {
            assertThat(fileStorage.exists("check.txt")).isFalse();

            byte[] content = "check".getBytes();
            fileStorage.upload(UploadRequest.of("check.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            assertThat(fileStorage.exists("check.txt")).isTrue();
        }
    }

    @Nested
    @DisplayName("delete 操作")
    class Delete {

        @Test
        @DisplayName("delete 应删除已存在的文件")
        void shouldDeleteExistingFile() {
            byte[] content = "delete me".getBytes();
            fileStorage.upload(UploadRequest.of("delete.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            boolean deleted = fileStorage.delete("delete.txt");
            assertThat(deleted).isTrue();
            assertThat(fileStorage.exists("delete.txt")).isFalse();
        }

        @Test
        @DisplayName("delete 不存在的文件应返回 false")
        void shouldReturnFalse_whenDeleteNonExistent() {
            boolean deleted = fileStorage.delete("nonexistent.txt");
            assertThat(deleted).isFalse();
        }
    }

    @Nested
    @DisplayName("deleteBatch 操作")
    class DeleteBatch {

        @Test
        @DisplayName("deleteBatch 应批量删除文件")
        void shouldDeleteBatch() {
            byte[] content = "batch".getBytes();
            fileStorage.upload(UploadRequest.of("batch1.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));
            fileStorage.upload(UploadRequest.of("batch2.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            int count = fileStorage.deleteBatch(java.util.List.of("batch1.txt", "batch2.txt", "batch3.txt"));
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("get 操作")
    class Get {

        @Test
        @DisplayName("get 应返回文件的 StorageObject")
        void shouldReturnStorageObject() {
            byte[] content = "get me".getBytes();
            fileStorage.upload(UploadRequest.of("get.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            StorageObject obj = fileStorage.get("get.txt");
            assertThat(obj).isNotNull();
            assertThat(obj.key()).isEqualTo("get.txt");
            assertThat(obj.size()).isEqualTo(content.length);
        }

        @Test
        @DisplayName("get 不存在的文件应返回 null")
        void shouldReturnNull_whenFileNotExists() {
            StorageObject obj = fileStorage.get("nonexistent.txt");
            assertThat(obj).isNull();
        }
    }

    @Nested
    @DisplayName("list 操作")
    class List {

        @Test
        @DisplayName("list 应返回目录下的文件列表")
        void shouldListFiles() {
            byte[] content = "list".getBytes();
            fileStorage.upload(UploadRequest.of("list1.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));
            fileStorage.upload(UploadRequest.of("list2.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            var result = fileStorage.list(ListOptions.defaults());

            assertThat(result.objects()).hasSize(2);
        }

        @Test
        @DisplayName("list 带 prefix 应只返回匹配前缀的文件")
        void shouldListWithPrefix() {
            byte[] content = "prefix".getBytes();
            fileStorage.upload(UploadRequest.of("prefix/a.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));
            fileStorage.upload(UploadRequest.of("prefix/b.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));
            fileStorage.upload(UploadRequest.of("other/c.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            var result = fileStorage.list(ListOptions.withPrefix("prefix/"));

            assertThat(result.objects()).hasSize(2);
        }

        @Test
        @DisplayName("list 空目录应返回空结果")
        void shouldReturnEmpty_whenDirectoryEmpty() {
            var result = fileStorage.list(ListOptions.defaults());

            assertThat(result.objects()).isEmpty();
        }
    }

    @Nested
    @DisplayName("copy 操作")
    class Copy {

        @Test
        @DisplayName("copy 应复制文件到新位置")
        void shouldCopyFile() throws IOException {
            byte[] content = "copy me".getBytes();
            fileStorage.upload(UploadRequest.of("source.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            StorageObject copied = fileStorage.copy("source.txt", "dest.txt");

            assertThat(copied.key()).isEqualTo("dest.txt");
            assertThat(fileStorage.exists("source.txt")).isTrue();
            assertThat(fileStorage.exists("dest.txt")).isTrue();

            DownloadResult result = fileStorage.download("dest.txt");
            assertThat(result.inputStream().readAllBytes()).isEqualTo(content);
        }

        @Test
        @DisplayName("copy 不存在的源文件应抛出 StorageException")
        void shouldThrowException_whenSourceNotExists() {
            assertThatThrownBy(() -> fileStorage.copy("nonexistent.txt", "dest.txt"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("getUrl 操作")
    class GetUrl {

        @Test
        @DisplayName("getUrl 应返回文件的访问 URL")
        void shouldReturnFileUrl() {
            byte[] content = "url".getBytes();
            fileStorage.upload(UploadRequest.of("url-test.txt", new ByteArrayInputStream(content), (long) content.length, "text/plain"));

            String url = fileStorage.getUrl("url-test.txt");
            assertThat(url).contains("test-bucket");
            assertThat(url).contains("url-test.txt");
        }

        @Test
        @DisplayName("getUrl 不存在的文件应抛出 StorageException")
        void shouldThrowException_whenFileNotExists() {
            assertThatThrownBy(() -> fileStorage.getUrl("nonexistent.txt"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("安全防护")
    class Security {

        @Test
        @DisplayName("路径遍历攻击应被阻止")
        void shouldBlockPathTraversal() {
            assertThatThrownBy(() -> fileStorage.download("../../etc/passwd"))
                    .isInstanceOf(StorageException.class);
        }
    }
}
