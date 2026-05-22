package io.github.afgprojects.framework.ai.rag.loader;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 默认编码检测器
 *
 * <p>使用 juniversalchardet 进行自动编码检测，支持多种中文编码。
 *
 * <p>检测策略：
 * <ol>
 *   <li>BOM 检测</li>
 *   <li>使用 juniversalchardet 自动检测</li>
 *   <li>多编码尝试解码验证</li>
 *   <li>默认返回 UTF-8</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultEncodingDetector implements EncodingDetector {

    private static final Logger log = LoggerFactory.getLogger(DefaultEncodingDetector.class);

    private static final Charset[] DEFAULT_CHARSETS = {
        StandardCharsets.UTF_8,
        Charset.forName("GBK"),
        Charset.forName("GB2312"),
        Charset.forName("GB18030"),
        StandardCharsets.ISO_8859_1,
        StandardCharsets.US_ASCII,
        StandardCharsets.UTF_16,
        StandardCharsets.UTF_16BE,
        StandardCharsets.UTF_16LE
    };

    @Override
    public @NonNull Charset detect(@NonNull Path path) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return detect(bytes);
        } catch (IOException e) {
            log.warn("Failed to read file for encoding detection: {}", path, e);
            return StandardCharsets.UTF_8;
        }
    }

    @Override
    public @NonNull Charset detect(@NonNull byte[] bytes) {
        // 1. BOM 检测
        Charset bomCharset = detectBom(bytes);
        if (bomCharset != null) {
            log.debug("Detected encoding via BOM: {}", bomCharset.name());
            return bomCharset;
        }

        // 2. 使用 juniversalchardet 自动检测
        UniversalDetector detector = new UniversalDetector(null);
        int detectLength = Math.min(bytes.length, 4096);
        detector.handleData(bytes, 0, detectLength);
        detector.dataEnd();
        String detected = detector.getDetectedCharset();

        if (detected != null) {
            try {
                Charset charset = Charset.forName(detected);
                // 验证解码是否成功
                if (tryDecode(bytes, charset) != null) {
                    log.debug("Detected encoding via universalchardet: {}", charset.name());
                    return charset;
                }
            } catch (Exception e) {
                log.debug("Failed to use detected charset: {}", detected);
            }
        }

        // 3. 尝试多种编码解码验证
        Charset validatedCharset = tryDecode(bytes, DEFAULT_CHARSETS);
        if (validatedCharset != null) {
            log.debug("Detected encoding via validation: {}", validatedCharset.name());
            return validatedCharset;
        }

        // 4. 默认 UTF-8
        log.debug("Using default encoding: UTF-8");
        return StandardCharsets.UTF_8;
    }

    @Override
    public @Nullable Charset tryDecode(@NonNull byte[] bytes, @NonNull Charset[] candidates) {
        for (Charset charset : candidates) {
            Charset validated = tryDecode(bytes, charset);
            if (validated != null) {
                return validated;
            }
        }
        return null;
    }

    /**
     * 尝试使用指定编码解码，验证是否成功
     */
    private @Nullable Charset tryDecode(byte[] bytes, Charset charset) {
        try {
            String decoded = new String(bytes, charset);
            // 检查是否有乱码
            if (!containsMojibake(decoded)) {
                return charset;
            }
        } catch (Exception e) {
            // 解码失败
        }
        return null;
    }

    /**
     * BOM 检测
     */
    private @Nullable Charset detectBom(byte[] bytes) {
        if (bytes.length < 2) {
            return null;
        }

        // UTF-8 BOM: EF BB BF
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            return StandardCharsets.UTF_8;
        }

        // UTF-16 BE BOM: FE FF
        if (bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
            return StandardCharsets.UTF_16BE;
        }

        // UTF-16 LE BOM: FF FE
        if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
            if (bytes.length >= 4 && bytes[2] == 0x00 && bytes[3] == 0x00) {
                // UTF-32 LE
                return Charset.forName("UTF-32LE");
            }
            return StandardCharsets.UTF_16LE;
        }

        // UTF-32 BE BOM: 00 00 FE FF
        if (bytes.length >= 4 && bytes[0] == 0x00 && bytes[1] == 0x00
            && bytes[2] == (byte) 0xFE && bytes[3] == (byte) 0xFF) {
            return Charset.forName("UTF-32BE");
        }

        return null;
    }

    /**
     * 检查文本是否包含乱码字符
     */
    private boolean containsMojibake(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // 替换字符 (U+FFFD)
            if (c == '�') {
                return true;
            }
            // 无效字符
            if (c == '￾' || c == '￿') {
                return true;
            }
        }
        return false;
    }
}
