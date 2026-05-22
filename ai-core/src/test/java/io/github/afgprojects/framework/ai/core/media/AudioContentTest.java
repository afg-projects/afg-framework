package io.github.afgprojects.framework.ai.core.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AudioContent 单元测试
 */
class AudioContentTest {

    @Test
    @DisplayName("从 URL 创建音频内容")
    void fromUrl() {
        String url = "https://example.com/audio.mp3";
        AudioContent audio = AudioContent.fromUrl(url);

        assertThat(audio.mimeType()).isEqualTo(AudioContent.MIME_TYPE_URL);
        assertThat(audio.data()).isEqualTo(url);
        assertThat(audio.isUrl()).isTrue();
        assertThat(audio.isBase64()).isFalse();
        assertThat(audio.isBytes()).isFalse();
        assertThat(audio.getUrl()).isEqualTo(url);
        assertThat(audio.getBase64()).isNull();
        assertThat(audio.getBytes()).isNull();
    }

    @Test
    @DisplayName("从 Base64 创建音频内容")
    void fromBase64() {
        String base64 = "//uQxAAAAAANIAAAAAExBTUUzLjEwMFVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV";
        String mimeType = "audio/mp3";
        AudioContent audio = AudioContent.fromBase64(base64, mimeType);

        assertThat(audio.mimeType()).isEqualTo(mimeType);
        assertThat(audio.data()).isEqualTo(base64);
        assertThat(audio.isUrl()).isFalse();
        assertThat(audio.isBase64()).isTrue();
        assertThat(audio.isBytes()).isFalse();
        assertThat(audio.getUrl()).isNull();
        assertThat(audio.getBase64()).isEqualTo(base64);
        assertThat(audio.getBytes()).isNull();
    }

    @Test
    @DisplayName("从字节数组创建音频内容")
    void fromBytes() {
        byte[] bytes = new byte[]{0x49, 0x44, 0x33}; // ID3 header for MP3
        String mimeType = "audio/mp3";
        AudioContent audio = AudioContent.fromBytes(bytes, mimeType);

        assertThat(audio.mimeType()).isEqualTo(mimeType);
        assertThat(audio.data()).isEqualTo(bytes);
        assertThat(audio.isUrl()).isFalse();
        assertThat(audio.isBase64()).isFalse();
        assertThat(audio.isBytes()).isTrue();
        assertThat(audio.getUrl()).isNull();
        assertThat(audio.getBase64()).isNull();
        assertThat(audio.getBytes()).isEqualTo(bytes);
    }

    @Test
    @DisplayName("URL 为空时抛出异常")
    void fromUrl_nullUrl() {
        assertThatThrownBy(() -> AudioContent.fromUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("url cannot be null or blank");
    }

    @Test
    @DisplayName("Base64 为空时抛出异常")
    void fromBase64_nullBase64() {
        assertThatThrownBy(() -> AudioContent.fromBase64(null, "audio/mp3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("base64 cannot be null or blank");
    }

    @Test
    @DisplayName("MIME type 为空时抛出异常")
    void fromBase64_nullMimeType() {
        assertThatThrownBy(() -> AudioContent.fromBase64("abc123", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mimeType cannot be null or blank");
    }

    @Test
    @DisplayName("字节数组为空时抛出异常")
    void fromBytes_nullBytes() {
        assertThatThrownBy(() -> AudioContent.fromBytes(null, "audio/mp3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bytes cannot be null or empty");
    }

    @Test
    @DisplayName("toString 包含预览信息")
    void toString_preview() {
        AudioContent urlAudio = AudioContent.fromUrl("https://example.com/audio.mp3");
        assertThat(urlAudio.toString()).contains("audio/url");
        assertThat(urlAudio.toString()).contains("https://example.com/audio.mp3");

        String longBase64 = "a".repeat(100);
        AudioContent base64Audio = AudioContent.fromBase64(longBase64, "audio/mp3");
        assertThat(base64Audio.toString()).contains("audio/mp3");
        assertThat(base64Audio.toString()).contains("...");

        byte[] bytes = new byte[100];
        AudioContent bytesAudio = AudioContent.fromBytes(bytes, "audio/mp3");
        assertThat(bytesAudio.toString()).contains("byte[100]");
    }
}
