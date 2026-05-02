package io.github.afgprojects.framework.core.api.storage.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * UploadRequest 测试
 */
@DisplayName("UploadRequest 测试")
class UploadRequestTest {

    @Nested
    @DisplayName("静态工厂方法测试")
    class StaticFactoryTests {

        @Test
        @DisplayName("应该创建简化上传请求")
        void shouldCreateSimpleUploadRequest() {
            ByteArrayInputStream stream = new ByteArrayInputStream("test".getBytes());
            UploadRequest request = UploadRequest.of("test.txt", stream);

            assertThat(request.key()).isEqualTo("test.txt");
            assertThat(request.inputStream()).isSameAs(stream);
            assertThat(request.size()).isEqualTo(-1);
            assertThat(request.contentType()).isNull();
            assertThat(request.metadata()).isNull();
        }

        @Test
        @DisplayName("应该创建带大小和类型的上传请求")
        void shouldCreateUploadRequestWithSizeAndType() {
            ByteArrayInputStream stream = new ByteArrayInputStream("test".getBytes());
            UploadRequest request = UploadRequest.of("test.txt", stream, 4, "text/plain");

            assertThat(request.key()).isEqualTo("test.txt");
            assertThat(request.inputStream()).isSameAs(stream);
            assertThat(request.size()).isEqualTo(4);
            assertThat(request.contentType()).isEqualTo("text/plain");
            assertThat(request.metadata()).isNull();
        }
    }

    @Nested
    @DisplayName("Builder 测试")
    class BuilderTests {

        @Test
        @DisplayName("应该使用 Builder 构建请求")
        void shouldBuildWithBuilder() {
            ByteArrayInputStream stream = new ByteArrayInputStream("content".getBytes());
            StorageMetadata metadata = StorageMetadata.builder().put("author", "test").build();

            UploadRequest request = UploadRequest.builder()
                    .key("file.pdf")
                    .inputStream(stream)
                    .size(7)
                    .contentType("application/pdf")
                    .metadata(metadata)
                    .build();

            assertThat(request.key()).isEqualTo("file.pdf");
            assertThat(request.inputStream()).isSameAs(stream);
            assertThat(request.size()).isEqualTo(7);
            assertThat(request.contentType()).isEqualTo("application/pdf");
            assertThat(request.metadata()).isSameAs(metadata);
        }

        @Test
        @DisplayName("应该在没有 key 时抛出异常")
        void shouldThrowWhenKeyMissing() {
            ByteArrayInputStream stream = new ByteArrayInputStream("test".getBytes());

            assertThatThrownBy(() -> UploadRequest.builder()
                    .inputStream(stream)
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("key and inputStream are required");
        }

        @Test
        @DisplayName("应该在没有 inputStream 时抛出异常")
        void shouldThrowWhenStreamMissing() {
            assertThatThrownBy(() -> UploadRequest.builder()
                    .key("test.txt")
                    .build())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("key and inputStream are required");
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

            UploadRequest request1 = UploadRequest.of("test.txt", stream1, 4, "text/plain");
            UploadRequest request2 = UploadRequest.of("test.txt", stream2, 4, "text/plain");

            assertThat(request1.key()).isEqualTo(request2.key());
            assertThat(request1.size()).isEqualTo(request2.size());
            assertThat(request1.contentType()).isEqualTo(request2.contentType());
        }
    }
}