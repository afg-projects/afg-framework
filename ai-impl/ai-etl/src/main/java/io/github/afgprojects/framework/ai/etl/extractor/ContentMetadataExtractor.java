package io.github.afgprojects.framework.ai.etl.extractor;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.etl.transformer.MetadataExtractor;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * 内容元数据提取器。
 *
 * <p>从文档内容中提取统计信息，如字符数、行数、词数等。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class ContentMetadataExtractor implements MetadataExtractor {

    @Override
    public @NonNull Map<String, Object> extract(@NonNull Document document) {
        Map<String, Object> metadata = new HashMap<>();
        String content = document.content();

        // 字符数
        metadata.put("charCount", content.length());

        // 行数
        int lineCount = content.isEmpty() ? 0 : content.split("\n").length;
        metadata.put("lineCount", lineCount);

        // 词数（简单统计，按空格分割）
        int wordCount = content.isEmpty() ? 0 :
            content.trim().split("\\s+").length;
        metadata.put("wordCount", wordCount);

        // 平均行长
        if (lineCount > 0 && !content.isEmpty()) {
            double avgLineLength = (double) content.length() / lineCount;
            metadata.put("avgLineLength", Math.round(avgLineLength));
        }

        // 是否包含中文字符
        boolean hasChinese = content.chars().anyMatch(c -> c >= 0x4E00 && c <= 0x9FFF);
        metadata.put("hasChinese", hasChinese);

        // 内容哈希（用于去重）
        int contentHash = content.hashCode();
        metadata.put("contentHash", contentHash);

        return metadata;
    }

    @Override
    public @NonNull String getName() {
        return "ContentMetadataExtractor";
    }
}