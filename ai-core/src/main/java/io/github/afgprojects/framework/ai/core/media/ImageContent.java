package io.github.afgprojects.framework.ai.core.media;

import org.jspecify.annotations.NonNull;

/**
 * Represents image content in a message.
 * <p>
 * ImageContent supports two formats:
 * <ul>
 *   <li>URL - a publicly accessible image URL</li>
 *   <li>Base64 - base64 encoded image data</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // From URL
 * ImageContent fromUrl = ImageContent.fromUrl("https://example.com/image.png");
 *
 * // From Base64
 * ImageContent fromBase64 = ImageContent.fromBase64("iVBORw0KGgo...", "image/png");
 *
 * // From file bytes
 * byte[] imageBytes = Files.readAllBytes(Paths.get("image.png"));
 * ImageContent fromBytes = ImageContent.fromBytes(imageBytes, "image/png");
 * }</pre>
 *
 * @param mimeType the MIME type (e.g., "image/png", "image/jpeg")
 * @param data     the image data (URL string, Base64 string, or byte array)
 * @author AFG Projects
 * @since 1.0.0
 */
public record ImageContent(
    @NonNull String mimeType,
    @NonNull Object data
) implements MediaContent {

    /**
     * MIME type for URL-based images.
     */
    public static final String MIME_TYPE_URL = "image/url";

    /**
     * Creates an ImageContent with validated parameters.
     *
     * @param mimeType the MIME type
     * @param data     the image data
     * @throws IllegalArgumentException if mimeType or data is null
     */
    public ImageContent {
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        if (data == null) {
            throw new IllegalArgumentException("data cannot be null");
        }
    }

    /**
     * Creates an ImageContent from a URL.
     * <p>
     * The URL should be publicly accessible for the LLM to fetch the image.
     *
     * @param url the image URL (must start with http:// or https://)
     * @return a new ImageContent with URL data
     * @throws IllegalArgumentException if url is null or blank
     */
    @NonNull
    public static ImageContent fromUrl(@NonNull String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("url cannot be null or blank");
        }
        return new ImageContent(MIME_TYPE_URL, url);
    }

    /**
     * Creates an ImageContent from Base64 encoded data.
     * <p>
     * The Base64 string should be the raw image data encoded in Base64,
     * without any data URI prefix.
     *
     * @param base64   the Base64 encoded image data
     * @param mimeType the MIME type (e.g., "image/png", "image/jpeg")
     * @return a new ImageContent with Base64 data
     * @throws IllegalArgumentException if base64 or mimeType is null/blank
     */
    @NonNull
    public static ImageContent fromBase64(@NonNull String base64, @NonNull String mimeType) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("base64 cannot be null or blank");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        return new ImageContent(mimeType, base64);
    }

    /**
     * Creates an ImageContent from raw bytes.
     * <p>
     * The bytes will be stored directly. When converting to LLM-specific formats,
     * the implementation may convert to Base64 as needed.
     *
     * @param bytes    the raw image bytes
     * @param mimeType the MIME type (e.g., "image/png", "image/jpeg")
     * @return a new ImageContent with byte data
     * @throws IllegalArgumentException if bytes is null or empty
     */
    @NonNull
    public static ImageContent fromBytes(@NonNull byte[] bytes, @NonNull String mimeType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes cannot be null or empty");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new IllegalArgumentException("mimeType cannot be null or blank");
        }
        return new ImageContent(mimeType, bytes);
    }

    /**
     * Gets the image URL if this is a URL-based image.
     *
     * @return the URL string, or null if not URL-based
     */
    public String getUrl() {
        return isUrl() ? (String) data() : null;
    }

    /**
     * Gets the Base64 data if this is a Base64-encoded image.
     *
     * @return the Base64 string, or null if not Base64
     */
    public String getBase64() {
        return isBase64() ? (String) data() : null;
    }

    /**
     * Gets the raw bytes if this is a byte-based image.
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
        return "ImageContent{mimeType='" + mimeType + "', data=" + dataPreview + "}";
    }
}
