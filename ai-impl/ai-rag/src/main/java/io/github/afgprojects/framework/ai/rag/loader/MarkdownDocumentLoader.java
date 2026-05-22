package io.github.afgprojects.framework.ai.rag.loader;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.DocumentLoader;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Markdown 文档加载器
 *
 * <p>基于 CommonMark 实现，支持 GFM 表格扩展。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class MarkdownDocumentLoader implements DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(MarkdownDocumentLoader.class);

    private final EncodingDetector encodingDetector;
    private final Parser parser;
    private final MarkdownLoadOptions options;

    public MarkdownDocumentLoader() {
        this(MarkdownLoadOptions.defaults());
    }

    public MarkdownDocumentLoader(MarkdownLoadOptions options) {
        this.encodingDetector = new DefaultEncodingDetector();
        this.options = options;
        this.parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    }

    @Override
    public @NonNull List<Document> load(@NonNull Source source) {
        Path path = Path.of(source.getPath());

        if (!Files.exists(path)) {
            throw new RuntimeException("Markdown file not found: " + path);
        }

        try {
            // 检测编码
            Charset charset = encodingDetector.detect(path);
            log.debug("Loading markdown file {} with encoding {}", path, charset.name());

            // 读取内容
            String content = Files.readString(path, charset);

            // 解析 Markdown
            Node document = parser.parse(content);

            // 提取标题作为文档标题
            String title = extractTitle(document);

            if (options.splitBySection()) {
                return splitBySection(document, path.toString(), title);
            } else {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", path.toString());
                metadata.put("contentType", "text/markdown");
                metadata.put("encoding", charset.name());
                if (title != null) {
                    metadata.put("title", title);
                }

                Document doc = new Document(
                    UUID.randomUUID().toString(),
                    content,
                    null,
                    metadata
                );
                return List.of(doc);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load markdown file: " + path, e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".md") || path.endsWith(".markdown") ||
               source.getContentType() != null && source.getContentType().contains("markdown");
    }

    /**
     * 提取文档标题（第一个 H1）
     */
    private String extractTitle(Node document) {
        HeadingVisitor visitor = new HeadingVisitor(1);
        visitor.visit(document);
        return visitor.getTitle();
    }

    /**
     * 按章节分割 Markdown
     */
    private List<Document> splitBySection(Node document, String filePath, String docTitle) {
        List<Document> sections = new ArrayList<>();
        SectionVisitor visitor = new SectionVisitor(filePath, docTitle);
        visitor.visit(document);
        return visitor.getSections();
    }

    /**
     * Markdown 加载选项
     */
    public record MarkdownLoadOptions(
        boolean splitBySection
    ) {
        public static MarkdownLoadOptions defaults() {
            return new MarkdownLoadOptions(false);
        }

        public static MarkdownLoadOptions bySection() {
            return new MarkdownLoadOptions(true);
        }
    }

    /**
     * 标题访问器
     */
    private static class HeadingVisitor {
        private final int level;
        private String title;

        HeadingVisitor(int level) {
            this.level = level;
        }

        void visit(Node node) {
            if (node instanceof Heading heading && heading.getLevel() == level) {
                StringBuilder sb = new StringBuilder();
                for (Node child = heading.getFirstChild(); child != null; child = child.getNext()) {
                    if (child instanceof Text text) {
                        sb.append(text.getLiteral());
                    }
                }
                title = sb.toString().trim();
                return;
            }
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                visit(child);
                if (title != null) return;
            }
        }

        String getTitle() {
            return title;
        }
    }

    /**
     * 章节访问器
     */
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private static class SectionVisitor {
        private final String filePath;
        private final String docTitle;
        private final List<Document> sections = new ArrayList<>();
        private final StringBuilder currentContent = new StringBuilder();
        private String currentHeading = "";
        private int sectionIndex = 0;

        SectionVisitor(String filePath, String docTitle) {
            this.filePath = filePath;
            this.docTitle = docTitle;
        }

        void visit(Node node) {
            if (node instanceof Heading heading) {
                // 保存当前章节
                if (currentContent.length() > 0) {
                    saveSection();
                }
                currentContent.setLength(0);  // 清空而不是重新创建
                currentHeading = extractHeadingText(heading);
                sectionIndex++;
            } else if (node instanceof Paragraph paragraph) {
                appendParagraph(paragraph);
            } else if (node instanceof FencedCodeBlock codeBlock) {
                appendCodeBlock(codeBlock);
            } else if (node instanceof BlockQuote blockQuote) {
                appendBlockQuote(blockQuote);
            } else if (node instanceof ListBlock listBlock) {
                appendList(listBlock);
            } else {
                // 递归处理子节点
                for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                    visit(child);
                }
            }
        }

        private void appendParagraph(Paragraph paragraph) {
            for (Node child = paragraph.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Text text) {
                    currentContent.append(text.getLiteral());
                } else if (child instanceof Emphasis emphasis) {
                    currentContent.append("*").append(extractText(emphasis)).append("*");
                } else if (child instanceof StrongEmphasis strong) {
                    currentContent.append("**").append(extractText(strong)).append("**");
                } else if (child instanceof Code code) {
                    currentContent.append("`").append(code.getLiteral()).append("`");
                } else if (child instanceof Link link) {
                    currentContent.append("[").append(extractText(link)).append("](").append(link.getDestination()).append(")");
                }
            }
            currentContent.append("\n\n");
        }

        private void appendCodeBlock(FencedCodeBlock codeBlock) {
            currentContent.append("```").append(codeBlock.getInfo() != null ? codeBlock.getInfo() : "").append("\n");
            currentContent.append(codeBlock.getLiteral()).append("\n```\n\n");
        }

        private void appendBlockQuote(BlockQuote blockQuote) {
            currentContent.append("> ");
            for (Node child = blockQuote.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Paragraph paragraph) {
                    appendParagraph(paragraph);
                }
            }
        }

        private void appendList(ListBlock listBlock) {
            for (Node item = listBlock.getFirstChild(); item != null; item = item.getNext()) {
                if (item instanceof ListItem listItem) {
                    if (listBlock instanceof BulletList) {
                        currentContent.append("- ");
                    } else if (listBlock instanceof OrderedList orderedList) {
                        currentContent.append(orderedList.getStartNumber()).append(". ");
                    }
                    for (Node child = listItem.getFirstChild(); child != null; child = child.getNext()) {
                        if (child instanceof Paragraph paragraph) {
                            appendParagraph(paragraph);
                        }
                    }
                }
            }
        }

        private String extractText(Node node) {
            StringBuilder sb = new StringBuilder();
            for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Text text) {
                    sb.append(text.getLiteral());
                }
            }
            return sb.toString();
        }

        private String extractHeadingText(Heading heading) {
            StringBuilder sb = new StringBuilder();
            for (Node child = heading.getFirstChild(); child != null; child = child.getNext()) {
                if (child instanceof Text text) {
                    sb.append(text.getLiteral());
                }
            }
            return sb.toString().trim();
        }

        private void saveSection() {
            String content = currentContent.toString().trim();
            if (!content.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", filePath);
                metadata.put("contentType", "text/markdown");
                metadata.put("sectionIndex", sectionIndex);
                if (currentHeading != null && !currentHeading.isEmpty()) {
                    metadata.put("sectionHeading", currentHeading);
                }
                if (docTitle != null) {
                    metadata.put("documentTitle", docTitle);
                }

                Document doc = new Document(
                    UUID.randomUUID().toString(),
                    content,
                    null,
                    metadata
                );
                sections.add(doc);
            }
        }

        List<Document> getSections() {
            // 保存最后一个章节
            if (currentContent.length() > 0) {
                saveSection();
            }
            return sections;
        }
    }
}