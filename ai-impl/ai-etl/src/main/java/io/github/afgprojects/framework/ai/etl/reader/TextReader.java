package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.DocumentReader;
import io.github.afgprojects.framework.ai.core.etl.Source;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 纯文本文档读取器。
 *
 * <p>支持自动编码检测，可加载 .txt、.text 等纯文本文件。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class TextReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(TextReader.class);

    private final EncodingDetector encodingDetector;

    /**
     * 创建默认配置的文本读取器。
     */
    public TextReader() {
        this.encodingDetector = new DefaultEncodingDetector();
    }

    /**
     * 创建带指定编码检测器的文本读取器。
     *
     * @param encodingDetector 编码检测器
     */
    public TextReader(EncodingDetector encodingDetector) {
        this.encodingDetector = encodingDetector;
    }

    @Override
    public @NonNull List<Document> read(@NonNull Source source) {
        Path path = Path.of(source.getPath());

        if (!Files.exists(path)) {
            throw new RuntimeException("Text file not found: " + source.getPath());
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            Charset charset = encodingDetector.detect(bytes);
            String content = new String(bytes, charset);

            log.debug("Reading text file {} with encoding {}", path, charset.name());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source.getPath());
            metadata.put("contentType", "text/plain");
            metadata.put("fileName", path.getFileName().toString());
            metadata.put("size", bytes.length);

            Document document = new Document(
                UUID.randomUUID().toString(),
                content,
                null,
                metadata
            );

            return List.of(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read text file: " + source.getPath(), e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".txt") || path.endsWith(".text") ||
               "text/plain".equals(source.getContentType());
    }
}