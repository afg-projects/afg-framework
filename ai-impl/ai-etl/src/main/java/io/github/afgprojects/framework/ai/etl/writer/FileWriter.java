package io.github.afgprojects.framework.ai.etl.writer;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * 文件写入器。
 *
 * <p>将文档写入本地文件系统，支持 JSON 和纯文本格式。
 *
 * <p>文件名使用文档 ID，确保唯一性。支持追加模式和覆盖模式。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class FileWriter implements DocumentWriter {

    private static final Logger log = LoggerFactory.getLogger(FileWriter.class);

    /**
     * 输出格式。
     */
    public enum OutputFormat {
        /**
         * JSON 格式，包含完整文档信息（ID、内容、嵌入、元数据）。
         */
        JSON,

        /**
         * 纯文本格式，仅包含文档内容。
         */
        TEXT
    }

    private final Path outputDir;
    private final OutputFormat format;
    private final boolean append;
    private final String fileExtension;

    /**
     * 创建文件写入器（默认 JSON 格式，覆盖模式）。
     *
     * @param outputDir 输出目录
     */
    public FileWriter(@NonNull Path outputDir) {
        this(outputDir, OutputFormat.JSON, false);
    }

    /**
     * 创建文件写入器。
     *
     * @param outputDir 输出目录
     * @param format    输出格式
     * @param append    是否追加模式
     */
    public FileWriter(@NonNull Path outputDir, @NonNull OutputFormat format, boolean append) {
        this.outputDir = outputDir;
        this.format = format;
        this.append = append;
        this.fileExtension = format == OutputFormat.JSON ? ".json" : ".txt";

        // 确保输出目录存在
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + outputDir, e);
        }
    }

    @Override
    public void write(@NonNull List<Document> documents) {
        log.debug("Writing {} documents to {}", documents.size(), outputDir);

        for (Document doc : documents) {
            try {
                writeDocument(doc);
            } catch (IOException e) {
                log.warn("Failed to write document {}: {}", doc.id(), e.getMessage());
            }
        }

        log.debug("Wrote {} documents", documents.size());
    }

    /**
     * 写入单个文档。
     */
    private void writeDocument(@NonNull Document doc) throws IOException {
        String filename = sanitizeFilename(doc.id()) + fileExtension;
        Path filePath = outputDir.resolve(filename);

        String content = format == OutputFormat.JSON ? toJson(doc) : doc.content();

        if (append) {
            Files.writeString(filePath, content + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } else {
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        log.trace("Wrote document {} to {}", doc.id(), filePath);
    }

    /**
     * 将文档转换为 JSON 字符串。
     */
    private String toJson(@NonNull Document doc) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"id\": \"").append(escapeJson(doc.id())).append("\",\n");
        sb.append("  \"content\": \"").append(escapeJson(doc.content())).append("\"");

        if (doc.embedding() != null && !doc.embedding().isEmpty()) {
            sb.append(",\n  \"embedding\": [");
            List<Double> embedding = doc.embedding();
            for (int i = 0; i < embedding.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(embedding.get(i));
            }
            sb.append("]");
        }

        if (!doc.metadata().isEmpty()) {
            sb.append(",\n  \"metadata\": {");
            boolean first = true;
            for (var entry : doc.metadata().entrySet()) {
                if (!first) sb.append(", ");
                first = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\": ");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(escapeJson(String.valueOf(entry.getValue()))).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
            }
            sb.append("}");
        }

        sb.append("\n}");
        return sb.toString();
    }

    /**
     * 转义 JSON 字符串中的特殊字符。
     */
    private String escapeJson(@NonNull String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * 清理文件名，移除不安全字符。
     */
    private String sanitizeFilename(@NonNull String filename) {
        // 替换不安全字符为下划线
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 获取输出目录。
     */
    @NonNull
    public Path getOutputDir() {
        return outputDir;
    }

    /**
     * 获取输出格式。
     */
    @NonNull
    public OutputFormat getFormat() {
        return format;
    }

    /**
     * 是否追加模式。
     */
    public boolean isAppend() {
        return append;
    }
}
