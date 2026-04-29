package io.github.afgprojects.framework.integration.storage.local;

import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;
import io.github.afgprojects.framework.integration.storage.model.StorageException;

/**
 * 本地文件存储测试
 */
class LocalFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalFileStorage storage;

    @BeforeEach
    void setUp() throws IOException {
        storage = new LocalFileStorage("test-bucket", tempDir.toString(), "http://localhost:8080/files");
    }

    @AfterEach
    void tearDown() throws IOException {
        // 清理临时目录
        if (storage != null) {
            // 清理测试文件
        }
    }

    @Test
    void shouldReturnCorrectStorageType() {
        assertThat(storage.getStorageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void shouldReturnCorrectBucket() {
        assertThat(storage.getBucket()).isEqualTo("test-bucket");
    }

    @Test
    void shouldUploadFile() {
        String content = "Hello, World!";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));

        StorageObject result = storage.upload("test.txt", inputStream, content.length(), "text/plain");

        assertThat(result).isNotNull();
        assertThat(result.key()).isEqualTo("test.txt");
        assertThat(result.size()).isEqualTo(content.length());
        assertThat(result.contentType()).isEqualTo("text/plain");
    }

    @Test
    void shouldUploadFileWithBuilder() {
        String content = "Hello, Builder!";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8));

        UploadRequest request = UploadRequest.builder()
                .key("builder/test.txt")
                .inputStream(inputStream)
                .size(content.length())
                .contentType("text/plain")
                .build();

        StorageObject result = storage.upload(request);

        assertThat(result).isNotNull();
        assertThat(result.key()).isEqualTo("builder/test.txt");
    }

    @Test
    void shouldDownloadFile() throws IOException {
        // 上传文件
        String content = "Download test content";
        storage.upload("download-test.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        // 下载文件
        try (var download = storage.download("download-test.txt")) {
            String downloadedContent = new String(download.inputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(downloadedContent).isEqualTo(content);
            assertThat(download.size()).isEqualTo(content.length());
            assertThat(download.contentType()).isEqualTo("text/plain");
        }
    }

    @Test
    void shouldThrowExceptionWhenDownloadNonExistentFile() {
        assertThatThrownBy(() -> storage.download("non-existent.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("文件不存在");
    }

    @Test
    void shouldDeleteFile() {
        // 上传文件
        String content = "Delete test";
        storage.upload("delete-test.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        // 确认存在
        assertThat(storage.exists("delete-test.txt")).isTrue();

        // 删除文件
        boolean deleted = storage.delete("delete-test.txt");

        assertThat(deleted).isTrue();
        assertThat(storage.exists("delete-test.txt")).isFalse();
    }

    @Test
    void shouldReturnFalseWhenDeleteNonExistentFile() {
        boolean deleted = storage.delete("non-existent.txt");
        assertThat(deleted).isFalse();
    }

    @Test
    void shouldCheckFileExists() {
        String content = "Exists test";
        storage.upload("exists-test.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        assertThat(storage.exists("exists-test.txt")).isTrue();
        assertThat(storage.exists("non-existent.txt")).isFalse();
    }

    @Test
    void shouldGetFileInfo() {
        String content = "Info test";
        storage.upload("info-test.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        StorageObject info = storage.get("info-test.txt");

        assertThat(info).isNotNull();
        assertThat(info.key()).isEqualTo("info-test.txt");
        assertThat(info.size()).isEqualTo(content.length());
        assertThat(info.contentType()).isEqualTo("text/plain");
    }

    @Test
    void shouldReturnNullForNonExistentFile() {
        StorageObject info = storage.get("non-existent.txt");
        assertThat(info).isNull();
    }

    @Test
    void shouldListFiles() {
        // 上传多个文件
        for (int i = 1; i <= 5; i++) {
            String content = "File " + i;
            storage.upload("list-test/file" + i + ".txt",
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    content.length(), "text/plain");
        }

        ListResult result = storage.list(ListOptions.withPrefix("list-test/"));

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.isTruncated()).isFalse();
    }

    @Test
    void shouldReturnEmptyListForNonExistentPrefix() {
        ListResult result = storage.list(ListOptions.withPrefix("non-existent/"));
        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void shouldCopyFile() throws IOException {
        // 上传源文件
        String content = "Copy test content";
        storage.upload("copy-source.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        // 复制文件
        StorageObject copied = storage.copy("copy-source.txt", "copy-target.txt");

        assertThat(copied).isNotNull();
        assertThat(copied.key()).isEqualTo("copy-target.txt");

        // 验证内容相同
        try (var download = storage.download("copy-target.txt")) {
            String downloadedContent = new String(download.inputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(downloadedContent).isEqualTo(content);
        }
    }

    @Test
    void shouldThrowExceptionWhenCopyNonExistentFile() {
        assertThatThrownBy(() -> storage.copy("non-existent.txt", "target.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("文件不存在");
    }

    @Test
    void shouldMoveFile() throws IOException {
        // 上传源文件
        String content = "Move test content";
        storage.upload("move-source.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        // 移动文件
        StorageObject moved = storage.move("move-source.txt", "move-target.txt");

        assertThat(moved).isNotNull();
        assertThat(moved.key()).isEqualTo("move-target.txt");
        assertThat(storage.exists("move-source.txt")).isFalse();
        assertThat(storage.exists("move-target.txt")).isTrue();
    }

    @Test
    void shouldGetUrl() {
        // 上传文件
        String content = "URL test";
        storage.upload("url-test.txt",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), "text/plain");

        String url = storage.getUrl("url-test.txt");

        assertThat(url).contains("test-bucket");
        assertThat(url).contains("url-test.txt");
    }

    @Test
    void shouldThrowExceptionWhenGetUrlForNonExistentFile() {
        assertThatThrownBy(() -> storage.getUrl("non-existent.txt"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("文件不存在");
    }

    @Test
    void shouldBatchDeleteFiles() {
        // 上传多个文件
        for (int i = 1; i <= 3; i++) {
            String content = "Batch delete " + i;
            storage.upload("batch-delete/file" + i + ".txt",
                    new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                    content.length(), "text/plain");
        }

        // 批量删除
        int deleted = storage.deleteBatch(java.util.List.of(
                "batch-delete/file1.txt", "batch-delete/file2.txt", "batch-delete/file3.txt"));

        assertThat(deleted).isEqualTo(3);
        assertThat(storage.exists("batch-delete/file1.txt")).isFalse();
        assertThat(storage.exists("batch-delete/file2.txt")).isFalse();
        assertThat(storage.exists("batch-delete/file3.txt")).isFalse();
    }

    @Test
    void shouldPreventPathTraversalAttack() {
        assertThatThrownBy(() -> storage.download("../../../etc/passwd"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("非法文件路径");
    }

    @Test
    void shouldGuessContentTypeFromExtension() throws IOException {
        // 上传无 content type 的文件
        String content = "{}";
        storage.upload("data.json",
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)),
                content.length(), null);

        StorageObject info = storage.get("data.json");
        assertThat(info.contentType()).isEqualTo("application/json");
    }
}
