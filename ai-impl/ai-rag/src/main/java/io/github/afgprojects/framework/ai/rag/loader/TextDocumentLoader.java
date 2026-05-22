package io.github.afgprojects.framework.ai.rag.loader;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.DocumentLoader;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 纯文本文档加载器
 *
 * <p>支持自动编码检测，可加载 .txt、.text 等纯文本文件。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class TextDocumentLoader implements DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(TextDocumentLoader.class);

    private final EncodingDetector encodingDetector;

    public TextDocumentLoader() {
        this(new DefaultEncodingDetector());
    }

    public TextDocumentLoader(EncodingDetector encodingDetector) {
        this.encodingDetector = encodingDetector;
    }

    @Override
    public @NonNull List<Document> load(@NonNull Source source) {
        Path path = Path.of(source.getPath());

        if (!Files.exists(path)) {
            throw new RuntimeException("File not found: " + path);
        }

        try {
            // 检测编码
            Charset charset = encodingDetector.detect(path);
            log.debug("Loading text file {} with encoding {}", path, charset.name());

            // 读取内容
            String content = Files.readString(path, charset);

            // 创建文档
            Document document = new Document(
                UUID.randomUUID().toString(),
                content,
                null,
                Map.of(
                    "source", path.toString(),
                    "contentType", source.getContentType() != null ? source.getContentType() : "text/plain",
                    "encoding", charset.name()
                )
            );

            return List.of(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text file: " + path, e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".txt") || path.endsWith(".text") ||
               source.getContentType() != null && source.getContentType().startsWith("text/plain");
    }
}
