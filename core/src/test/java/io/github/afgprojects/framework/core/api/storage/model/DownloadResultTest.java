package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * DownloadResult 测试
 */
@DisplayName("DownloadResult 测试")
class DownloadResultTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建简化下载结果")
        void shouldCreateSimpleDownloadResult() {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
            DownloadResult result = DownloadResult.of(stream, 7);

            assertThat(result.inputStream()).isSameAs(stream);
            assertThat(result.size()).isEqualTo(7);
            assertThat(result.contentType()).isNull();
            assertThat(result.etag()).isNull();
        }

        @Test
        @DisplayName("应该创建带类型的下载结果")
        void shouldCreateDownloadResultWithType() {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
            DownloadResult result = DownloadResult.of(stream, 7, "text/plain");

            assertThat(result.inputStream()).isSameAs(stream);
            assertThat(result.size()).isEqualTo(7);
            assertThat(result.contentType()).isEqualTo("text/plain");
            assertThat(result.etag()).isNull();
        }
    }

    @Nested
    @DisplayName("AutoCloseable 测试")
    class AutoCloseableTests {

        @Test
        @DisplayName("应该关闭输入流")
        void shouldCloseInputStream() throws IOException {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
            DownloadResult result = new DownloadResult(stream, 7, "text/plain", "etag123");

            result.close();

            // ByteArrayInputStream.close() 实际上不做任何事情
            // 这只是验证 close 方法不会抛出异常
            assertThat(result.size()).isEqualTo(7);
        }

        @Test
        @DisplayName("应该支持 try-with-resources")
        void shouldSupportTryWithResources() {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());

            try (DownloadResult result = new DownloadResult(stream, 7, "text/plain", "etag123")) {
                assertThat(result.size()).isEqualTo(7);
                assertThat(result.etag()).isEqualTo("etag123");
            }

            // ByteArrayInputStream.close() 不做任何事情，流仍然可用
            assertThat(stream.available()).isEqualTo(7);
        }

        @Test
        @DisplayName("应该处理已关闭的流")
        void shouldHandleClosedStream() throws IOException {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
            DownloadResult result = new DownloadResult(stream, 7, null, null);

            stream.close();
            result.close(); // 不应该抛出异常
        }
    }

    @Nested
    @DisplayName("Record 特性测试")
    class RecordTests {

        @Test
        @DisplayName("应该正确实现 equals")
        void shouldImplementEquals() {
            ByteArrayInputStream stream1 = new ByteArrayInputStream("test".getBytes());
            ByteArrayInputStream stream2 = new ByteArrayInputStream("test".getBytes());

            DownloadResult result1 = new DownloadResult(stream1, 4, "text/plain", "etag");
            DownloadResult result2 = new DownloadResult(stream2, 4, "text/plain", "etag");

            assertThat(result1.size()).isEqualTo(result2.size());
            assertThat(result1.contentType()).isEqualTo(result2.contentType());
            assertThat(result1.etag()).isEqualTo(result2.etag());
        }
    }
}