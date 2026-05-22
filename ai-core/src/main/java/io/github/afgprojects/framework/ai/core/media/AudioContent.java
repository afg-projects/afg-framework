package io.github.afgprojects.framework.ai.core.media;

import org.jspecify.annotations.NonNull;

/**
 * Represents audio content in a message.
 * <p>
 * AudioContent supports two formats:
 * <ul>
 *   <li>URL - a publicly accessible audio URL</li>
 *   <li>Base64 - base64 encoded audio data</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // From URL
 * AudioContent fromUrl = AudioContent.fromUrl("https://example.com/audio.mp3");
 *
 * // From Base64
 * AudioContent fromBase64 = AudioContent.fromBase64("//uQxAAAAA...", "audio/mp3");
 *
 * // From file bytes
 * byte[] audioBytes = Files.readAllBytes(Paths.get("audio.mp3"));
 * AudioContent fromBytes = AudioContent.fromBytes(audioBytes, "audio/mp3");
 * }</pre>
 *
 * @param mimeType the MIME type (e.g., "audio/mp3", "audio/wav")
 * @param data     the audio data (URL string, Base64 string, or byte array)
 * @author AFG Projects
 * @since 1.0.0
 */
public record AudioContent(
    @NonNull String mimeType,
    @NonNull Object data
) implements MediaContent {

    /**
     * MIME type for URL-based audio.
     */
    public static final String MIME_TYPE_URL = "audio/url";

    /**
     * Creates an AudioContent with validated parameters.
     *
     * @param mimeType the MIME type
     * @param data     the audio data
     * @throws IllegalArgumentException if mimeType or data is null
     */
    public AudioContent {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }

    /**
     * Creates an AudioContent from a URL.
     * <p>
     * The URL should be publicly accessible for the LLM to fetch the audio.
     *
     * @param url the audio URL (must start with http:// or https://)
     * @return a new AudioContent with URL data
     * @throws IllegalArgumentException if url is null or blank
     */
    @NonNull
    public static AudioContent fromUrl(@NonNull String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url cannot be null or blank");
        }
        return new AudioContent(MIME_TYPE_URL, url);
    }

    /**
     * Creates an AudioContent from Base64 encoded data.
     * <p>
     * The Base64 string should be the raw audio data encoded in Base64,
     * without any data URI prefix.
     *
     * @param base64   the Base64 encoded audio data
     * @param mimeType the MIME type (e.g., "audio/mp3", "audio/wav")
     * @return a new AudioContent with Base64 data
     * @throws IllegalArgumentException if base64 or mimeType is null/blank
     */
    @NonNull
    public static AudioContent fromBase64(@NonNull String base64, @NonNull String mimeType) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("base64 cannot be null or blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        return new AudioContent(mimeType, base64);
    }

    /**
     * Creates an AudioContent from raw bytes.
     * <p>
     * The bytes will be stored directly. When converting to LLM-specific formats,
     * the implementation may convert to Base64 as needed.
     *
     * @param bytes    the raw audio bytes
     * @param mimeType the MIME type (e.g., "audio/mp3", "audio/wav")
     * @return a new AudioContent with byte data
     * @throws IllegalArgumentException if bytes is null or empty
     */
    @NonNull
    public static AudioContent fromBytes(@NonNull byte[] bytes, @NonNull String mimeType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes cannot be null or empty");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        return new AudioContent(mimeType, bytes);
    }

    /**
     * Gets the audio URL if this is a URL-based audio.
     *
     * @return the URL string, or null if not URL-based
     */
    public String getUrl() {
        return isUrl() ? (String) data() : null;
    }

    /**
     * Gets the Base64 data if this is a Base64-encoded audio.
     *
     * @return the Base64 string, or null if not Base64
     */
    public String getBase64() {
        return isBase64() ? (String) data() : null;
    }

    /**
     * Gets the raw bytes if this is a byte-based audio.
     *
     * @return the byte array, or null if not byte-based
     */
    public byte[] getBytes() {
        return isBytes() ? (byte[]) data() : null;
    }

    @Override
    public String toString() {
        String dataPreview;
        if (isUrl()) {
            dataPreview = (String) data();
        } else if (isBase64()) {
            String base64 = (String) data();
            dataPreview = base64.length() > 50
                    ? base64.substring(0, 50) + "..."
                    : base64;
        } else {
            byte[] bytes = (byte[]) data();
            dataPreview = "byte[" + bytes.length + "]";
        }
        return "AudioContent{mimeType='" + mimeType + "', data=" + dataPreview + "}";
    }
}
