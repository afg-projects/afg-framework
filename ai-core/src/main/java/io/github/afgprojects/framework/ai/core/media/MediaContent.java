package io.github.afgprojects.framework.ai.core.media;

import org.jspecify.annotations.NonNull;

/**
 * Represents multimedia content in a message.
 * <p>
 * MediaContent is a sealed interface that permits specific media types
 * (currently ImageContent and AudioContent). This design ensures type safety
 * and allows exhaustive pattern matching.
 *
 * <p>Example usage:
 * <pre>{@code
 * // Create image from URL
 * MediaContent imageFromUrl = ImageContent.fromUrl("https://example.com/image.png");
 *
 * // Create image from Base64
 * MediaContent imageFromBase64 = ImageContent.fromBase64("base64data", "image/png");
 *
 * // Create audio from URL
 * MediaContent audioFromUrl = AudioContent.fromUrl("https://example.com/audio.mp3");
 *
 * // Pattern matching
 * String type = switch (media) {
 *     case ImageContent img -> "image: " + img.mimeType();
 *     case AudioContent aud -> "audio: " + aud.mimeType();
 * };
 * }</pre>
 *
 * @author AFG Projects
 * @since 1.0.0
 * @see ImageContent
 * @see AudioContent
 */
public sealed interface MediaContent permits ImageContent, AudioContent {

    /**
     * Gets the MIME type of the media content.
     * <p>
     * Common MIME types:
     * <ul>
     *   <li>Images: image/png, image/jpeg, image/gif, image/webp</li>
     *   <li>Audio: audio/mp3, audio/wav, audio/ogg, audio/m4a</li>
     * </ul>
     *
     * @return the MIME type string (never null)
     */
    @NonNull
    String mimeType();

    /**
     * Gets the media data.
     * <p>
     * The data can be:
     * <ul>
     *   <li>String - URL or Base64 encoded data</li>
     *   <li>byte[] - raw binary data</li>
     * </ul>
     *
     * @return the media data (never null)
     */
    @NonNull
    Object data();

    /**
     * Checks if the data is a URL.
     *
     * @return true if data is a URL string
     */
    default boolean isUrl() {
        Object d = data();
        return d instanceof String str && (str.startsWith("http://") || str.startsWith("https://"));
    }

    /**
     * Checks if the data is Base64 encoded.
     *
     * @return true if data is Base64 encoded string
     */
    default boolean isBase64() {
        Object d = data();
        return d instanceof String str && !isUrl();
    }

    /**
     * Checks if the data is raw bytes.
     *
     * @return true if data is byte array
     */
    default boolean isBytes() {
        return data() instanceof byte[];
    }
}
