package io.github.afgprojects.framework.ai.core.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ImageContent 单元测试
 */
class ImageContentTest {

    @Test
    @DisplayName("从 URL 创建图片内容")
    void fromUrl() {
        String url = "https://example.com/image.png";
        ImageContent image = ImageContent.fromUrl(url);

        assertThat(image.mimeType()).isEqualTo(ImageContent.MIME_TYPE_URL);
        assertThat(image.data()).isEqualTo(url);
        assertThat(image.isUrl()).isTrue();
        assertThat(image.isBase64()).isFalse();
        assertThat(image.isBytes()).isFalse();
        assertThat(image.getUrl()).isEqualTo(url);
        assertThat(image.getBase64()).isNull();
        assertThat(image.getBytes()).isNull();
    }

    @Test
    @DisplayName("从 Base64 创建图片内容")
    void fromBase64() {
        String base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";
        String mimeType = "image/png";
        ImageContent image = ImageContent.fromBase64(base64, mimeType);

        assertThat(image.mimeType()).isEqualTo(mimeType);
        assertThat(image.data()).isEqualTo(base64);
        assertThat(image.isUrl()).isFalse();
        assertThat(image.isBase64()).isTrue();
        assertThat(image.isBytes()).isFalse();
        assertThat(image.getUrl()).isNull();
        assertThat(image.getBase64()).isEqualTo(base64);
        assertThat(image.getBytes()).isNull();
    }

    @Test
    @DisplayName("从字节数组创建图片内容")
    void fromBytes() {
        byte[] bytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // PNG header
        String mimeType = "image/png";
        ImageContent image = ImageContent.fromBytes(bytes, mimeType);

        assertThat(image.mimeType()).isEqualTo(mimeType);
        assertThat(image.data()).isEqualTo(bytes);
        assertThat(image.isUrl()).isFalse();
        assertThat(image.isBase64()).isFalse();
        assertThat(image.isBytes()).isTrue();
        assertThat(image.getUrl()).isNull();
        assertThat(image.getBase64()).isNull();
        assertThat(image.getBytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("URL 为空时抛出异常")
    void fromUrl_nullUrl() {
        assertThatThrownBy(() -> ImageContent.fromUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null or blank");
    }

    @Test
    @DisplayName("Base64 为空时抛出异常")
    void fromBase64_nullBase64() {
        assertThatThrownBy(() -> ImageContent.fromBase64(null, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64 cannot be null or blank");
    }

    @Test
    @DisplayName("MIME type 为空时抛出异常")
    void fromBase64_nullMimeType() {
        assertThatThrownBy(() -> ImageContent.fromBase64("abc123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mimeType cannot be null or blank");
    }

    @Test
    @DisplayName("字节数组为空时抛出异常")
    void fromBytes_nullBytes() {
        assertThatThrownBy(() -> ImageContent.fromBytes(null, "image/png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytes cannot be null or empty");
    }

    @Test
    @DisplayName("toString 包含预览信息")
    void toString_preview() {
        ImageContent urlImage = ImageContent.fromUrl("https://example.com/image.png");
        assertThat(urlImage.toString()).contains("image/url");
        assertThat(urlImage.toString()).contains("https://example.com/image.png");

        String longBase64 = "a".repeat(100);
        ImageContent base64Image = ImageContent.fromBase64(longBase64, "image/png");
        assertThat(base64Image.toString()).contains("image/png");
        assertThat(base64Image.toString()).contains("...");

        byte[] bytes = new byte[100];
        ImageContent bytesImage = ImageContent.fromBytes(bytes, "image/png");
        assertThat(bytesImage.toString()).contains("byte[100]");
    }
}
