package io.github.afgprojects.framework.commons.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IoUtils 测试")
class IoUtilsTest {

    @Nested
    @DisplayName("readAsString() 方法")
    class ReadAsStringTests {

        @Test
        @DisplayName("应正确读取 InputStream 为字符串")
        void shouldReadInputStreamAsString() throws IOException {
            ByteArrayInputStream input = new ByteArrayInputStream("hello world".getBytes());

            assertThat(IoUtils.readAsString(input)).isEqualTo("hello world");
        }

        @Test
        @DisplayName("多行内容应正确读取")
        void shouldReadMultiLineContent() throws IOException {
            ByteArrayInputStream input = new ByteArrayInputStream("line1\nline2\nline3".getBytes());

            assertThat(IoUtils.readAsString(input)).isEqualTo("line1\nline2\nline3");
        }

        @Test
        @DisplayName("null InputStream 应返回 null")
        void shouldReturnNullForNullInputStream() throws IOException {
            assertThat(IoUtils.readAsString(null)).isNull();
        }
    }

    @Nested
    @DisplayName("copy() 方法")
    class CopyTests {

        @Test
        @DisplayName("应正确复制 InputStream 到 OutputStream")
        void shouldCopyInputStreamToOutputStream() throws IOException {
            byte[] data = "hello world".getBytes();
            ByteArrayInputStream input = new ByteArrayInputStream(data);
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            long copied = IoUtils.copy(input, output);

            assertThat(copied).isEqualTo(data.length);
            assertThat(output.toByteArray()).isEqualTo(data);
        }

        @Test
        @DisplayName("null 输入应返回 0")
        void shouldReturnZeroForNullInput() throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            assertThat(IoUtils.copy(null, output)).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("closeQuietly() 方法")
    class CloseQuietlyTests {

        @Test
        @DisplayName("应正常关闭 AutoCloseable，不抛异常")
        void shouldCloseAutoCloseableWithoutException() {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            // ByteArrayOutputStream.close() 是空操作，不会抛异常
            IoUtils.closeQuietly(output);
            // 验证 closeQuietly 本身不抛异常即可
        }

        @Test
        @DisplayName("关闭时抛异常的 AutoCloseable 应被安静吞掉")
        void shouldSwallowCloseException() {
            AutoCloseable closeableWithException = () -> {
                throw new RuntimeException("close failed");
            };
            // 应不抛异常
            IoUtils.closeQuietly(closeableWithException);
        }

        @Test
        @DisplayName("null 时应不做任何操作")
        void shouldDoNothingForNull() {
            IoUtils.closeQuietly(null);
        }
    }
}