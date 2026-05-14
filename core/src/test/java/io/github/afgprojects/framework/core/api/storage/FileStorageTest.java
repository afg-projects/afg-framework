package io.github.afgprojects.framework.core.api.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.afgprojects.framework.core.api.storage.model.DownloadResult;
import io.github.afgprojects.framework.core.api.storage.model.ListOptions;
import io.github.afgprojects.framework.core.api.storage.model.ListResult;
import io.github.afgprojects.framework.core.api.storage.model.PresignedUrlOptions;
import io.github.afgprojects.framework.core.api.storage.model.StorageMetadata;
import io.github.afgprojects.framework.core.api.storage.model.StorageObject;
import io.github.afgprojects.framework.core.api.storage.model.StorageType;
import io.github.afgprojects.framework.core.api.storage.model.UploadRequest;

/**
 * {@link FileStorage} 文件存储接口测试
 *
 * <p>测试文件存储接口的默认方法和基本操作：
 * <ul>
 *   <li>上传、下载、删除文件</li>
 *   <li>文件元数据管理</li>
 *   <li>预签名 URL 生成</li>
 *   <li>文件复制和移动</li>
 * </ul>
 *
 * @see FileStorage
 */
@DisplayName("FileStorage 测试")
class FileStorageTest {

    /**
     * 测试用 FileStorage 实现，用于验证接口默认方法
     */
    private static class TestFileStorage implements FileStorage {
        @Override
        public StorageType getStorageType() {
            return StorageType.LOCAL;
        }

        @Override
        public String getBucket() {
            return "test-bucket";
        }

        @Override
        public StorageObject upload(UploadRequest request) {
            return new StorageObject(request.key(), request.size(), request.contentType(), null, Instant.now(), null);
        }

        @Override
        public DownloadResult download(String key) {
            return DownloadResult.of(new ByteArrayInputStream("test".getBytes()), 4);
        }

        @Override
        public boolean delete(String key) {
            return true;
        }

        @Override
        public int deleteBatch(Iterable<String> keys) {
            return 1;
        }

        @Override
        public boolean exists(String key) {
            return true;
        }

        @Override
        public StorageObject get(String key) {
            return new StorageObject(key, 100, "text/plain", null, Instant.now(), null);
        }

        @Override
        public ListResult list(ListOptions options) {
            return ListResult.empty();
        }

        @Override
        public String getUrl(String key) {
            return "https://example.com/" + key;
        }

        @Override
        public String getPresignedUrl(String key, PresignedUrlOptions options) {
            return "https://example.com/presigned/" + key;
        }

        @Override
        public StorageObject updateMetadata(String key, StorageMetadata metadata) {
            return new StorageObject(key, 100, "text/plain", null, Instant.now(), metadata);
        }

        @Override
        public StorageObject copy(String sourceKey, String targetKey) {
            return new StorageObject(targetKey, 100, "text/plain", null, Instant.now(), null);
        }
    }

    /**
     * 接口默认方法测试
     */
    @Nested
    @DisplayName("默认方法测试")
    class DefaultMethodTests {

        /**
         * 测试简化的 upload 方法
         */
        @Test
        @DisplayName("upload 简化方法应该正常工作")
        void shouldWorkWithSimplifiedUpload() {
            FileStorage storage = new TestFileStorage();
            InputStream stream = new ByteArrayInputStream("test".getBytes());

            StorageObject result = storage.upload("test.txt", stream, 4, "text/plain");

            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("test.txt");
        }

        /**
         * 测试 listByPrefix 方法
         */
        @Test
        @DisplayName("listByPrefix 应该正常工作")
        void shouldWorkWithListByPrefix() {
            FileStorage storage = new TestFileStorage();

            ListResult result = storage.listByPrefix("images/");

            assertThat(result).isNotNull();
        }

        /**
         * 测试简化的 getPresignedUrl 方法
         */
        @Test
        @DisplayName("getPresignedUrl 简化方法应该正常工作")
        void shouldWorkWithSimplifiedPresignedUrl() {
            FileStorage storage = new TestFileStorage();

            String result = storage.getPresignedUrl("test.txt");

            assertThat(result).contains("test.txt");
        }

        /**
         * 测试 move 方法（基于 copy + delete 实现）
         */
        @Test
        @DisplayName("move 应该调用 copy 和 delete")
        void shouldCallCopyAndDelete() {
            FileStorage storage = spy(new TestFileStorage());

            StorageObject result = storage.move("old.txt", "new.txt");

            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("new.txt");
            verify(storage).copy("old.txt", "new.txt");
            verify(storage).delete("old.txt");
        }
    }

    /**
     * 接口方法测试
     */
    @Nested
    @DisplayName("接口方法测试")
    class InterfaceMethodTests {

        /**
         * 测试获取存储类型
         */
        @Test
        @DisplayName("应该正确获取存储类型")
        void shouldGetStorageType() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.getStorageType()).isEqualTo(StorageType.LOCAL);
        }

        /**
         * 测试获取存储桶名称
         */
        @Test
        @DisplayName("应该正确获取存储桶名称")
        void shouldGetBucket() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.getBucket()).isEqualTo("test-bucket");
        }

        /**
         * 测试上传文件
         */
        @Test
        @DisplayName("应该正确上传文件")
        void shouldUploadFile() {
            FileStorage storage = new TestFileStorage();
            UploadRequest request = UploadRequest.of("test.txt", new ByteArrayInputStream("test".getBytes()));

            StorageObject result = storage.upload(request);

            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("test.txt");
        }

        /**
         * 测试下载文件
         */
        @Test
        @DisplayName("应该正确下载文件")
        void shouldDownloadFile() {
            FileStorage storage = new TestFileStorage();

            DownloadResult result = storage.download("test.txt");

            assertThat(result).isNotNull();
        }

        /**
         * 测试删除文件
         */
        @Test
        @DisplayName("应该正确删除文件")
        void shouldDeleteFile() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.delete("test.txt")).isTrue();
        }

        /**
         * 测试批量删除文件
         */
        @Test
        @DisplayName("应该正确批量删除文件")
        void shouldDeleteBatch() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.deleteBatch(Collections.singletonList("test.txt"))).isEqualTo(1);
        }

        /**
         * 测试检查文件是否存在
         */
        @Test
        @DisplayName("应该正确检查文件是否存在")
        void shouldCheckExists() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.exists("test.txt")).isTrue();
        }

        /**
         * 测试获取文件信息
         */
        @Test
        @DisplayName("应该正确获取文件信息")
        void shouldGetFileInfo() {
            FileStorage storage = new TestFileStorage();

            StorageObject result = storage.get("test.txt");

            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("test.txt");
        }

        /**
         * 测试列出文件
         */
        @Test
        @DisplayName("应该正确列出文件")
        void shouldListFiles() {
            FileStorage storage = new TestFileStorage();

            ListResult result = storage.list(ListOptions.defaults());

            assertThat(result).isNotNull();
        }

        /**
         * 测试获取 URL
         */
        @Test
        @DisplayName("应该正确获取 URL")
        void shouldGetUrl() {
            FileStorage storage = new TestFileStorage();

            assertThat(storage.getUrl("test.txt")).contains("test.txt");
        }

        /**
         * 测试更新元数据
         */
        @Test
        @DisplayName("应该正确更新元数据")
        void shouldUpdateMetadata() {
            FileStorage storage = new TestFileStorage();
            StorageMetadata metadata = new StorageMetadata();
            metadata.put("key", "value");

            StorageObject result = storage.updateMetadata("test.txt", metadata);

            assertThat(result).isNotNull();
        }

        /**
         * 测试复制文件
         */
        @Test
        @DisplayName("应该正确复制文件")
        void shouldCopyFile() {
            FileStorage storage = new TestFileStorage();

            StorageObject result = storage.copy("old.txt", "new.txt");

            assertThat(result).isNotNull();
            assertThat(result.key()).isEqualTo("new.txt");
        }
    }
}
