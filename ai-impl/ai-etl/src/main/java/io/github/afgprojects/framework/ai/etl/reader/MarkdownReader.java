package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.etl.Source;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
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
 * Markdown 文档读取器。
 *
 * <p>基于 CommonMark 实现，支持 GFM 表格扩展。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class MarkdownReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(MarkdownReader.class);

    private final EncodingDetector encodingDetector;
    private final Parser parser;
    private final MarkdownReadOptions options;

    /**
     * 创建默认配置的 Markdown 读取器。
     */
    public MarkdownReader() {
        this(MarkdownReadOptions.defaults());
    }

    /**
     * 创建带指定选项的 Markdown 读取器。
     *
     * @param options 读取选项
     */
    public MarkdownReader(MarkdownReadOptions options) {
        this.encodingDetector = new DefaultEncodingDetector();
        this.options = options;
        this.parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    }

    @Override
    public @NonNull List<Document> read(@NonNull Source source) {
        Path path = Path.of(source.getPath());

        if (!Files.exists(path)) {
            throw new RuntimeException("Markdown file not found: " + source.getPath());
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            Charset charset = encodingDetector.detect(path);
            String content = new String(bytes, charset);

            log.debug("Reading markdown file {} with encoding {}", path, charset.name());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", source.getPath());
            metadata.put("contentType", "text/markdown");
            metadata.put("fileName", path.getFileName().toString());

            if (options.extractTitle()) {
                String title = extractTitle(content);
                if (title != null) {
                    metadata.put("title", title);
                }
            }

            Document document = new Document(
                UUID.randomUUID().toString(),
                content,
                null,
                metadata
            );

            return List.of(document);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Markdown: " + source.getPath(), e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".md") || path.endsWith(".markdown") ||
               "text/markdown".equals(source.getContentType());
    }

    /**
     * 提取文档标题（第一个 H1）。
     */
    private String extractTitle(String content) {
        Node document = parser.parse(content);
        Heading firstHeading = findFirstHeading(document);
        if (firstHeading != null && firstHeading.getLevel() == 1) {
            return extractText(firstHeading);
        }
        return null;
    }

    /**
     * 查找第一个标题节点。
     */
    private Heading findFirstHeading(Node node) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Heading heading) {
                return heading;
            }
            Heading found = findFirstHeading(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * 提取节点的文本内容。
     */
    private String extractText(Node node) {
        StringBuilder sb = new StringBuilder();
        for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
            if (child instanceof Text text) {
                sb.append(text.getLiteral());
            }
        }
        return sb.toString().trim();
    }

    /**
     * Markdown 读取选项。
     *
     * @param extractTitle 是否提取文档标题（第一个 H1）
     */
    public record MarkdownReadOptions(
        boolean extractTitle
    ) {
        /**
         * 默认选项：提取标题。
         */
        public static MarkdownReadOptions defaults() {
            return new MarkdownReadOptions(true);
        }
    }
}