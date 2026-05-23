package io.github.afgprojects.framework.ai.etl.extractor;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.etl.transformer.MetadataExtractor;
import org.jspecify.annotations.NonNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件元数据提取器。
 *
 * <p>从文档的 source 元数据中提取文件相关信息。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class FileMetadataExtractor implements MetadataExtractor {

    @Override
    public @NonNull Map<String, Object> extract(@NonNull Document document) {
        Map<String, Object> metadata = new HashMap<>();

        Object source = document.getMetadata("source");
        if (source instanceof String sourcePath) {
            try {
                Path path = Path.of(sourcePath);
                Path fileName = path.getFileName();

                if (fileName != null) {
                    metadata.put("fileName", fileName.toString());

                    // 提取文件扩展名
                    String name = fileName.toString();
                    int dotIndex = name.lastIndexOf('.');
                    if (dotIndex > 0 && dotIndex < name.length() - 1) {
                        metadata.put("fileExtension", name.substring(dotIndex + 1).toLowerCase());
                    }
                }

                // 提取父目录
                Path parent = path.getParent();
                if (parent != null) {
                    metadata.put("directory", parent.toString());
                }
            } catch (Exception e) {
                // 忽略无效路径
            }
        }

        return metadata;
    }

    @Override
    public @NonNull String getName() {
        return "FileMetadataExtractor";
    }
}
