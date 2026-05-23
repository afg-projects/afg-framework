package io.github.afgprojects.framework.ai.core.etl;

import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档读取器接口。
 *
 * <p>负责从各种数据源读取文档，如文件、URL、数据库等。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public interface DocumentReader {

    /**
     * 从指定源读取文档。
     *
     * @param source 数据源
     * @return 读取的文档列表
     */
    @NonNull
    List<Document> read(@NonNull Source source);

    /**
     * 检查是否支持该数据源。
     *
     * @param source 数据源
     * @return 是否支持
     */
    boolean supports(@NonNull Source source);

    /**
     * 批量读取多个源。
     *
     * @param sources 数据源列表
     * @return 所有读取的文档
     */
    default @NonNull List<Document> readAll(@NonNull List<Source> sources) {
        List<Document> allDocuments = new ArrayList<>();
        for (Source source : sources) {
            if (supports(source)) {
                allDocuments.addAll(read(source));
            }
        }
        return allDocuments;
    }

    /**
     * 读取目录下所有文件。
     *
     * @param directory 目录路径
     * @param recursive 是否递归读取子目录
     * @return 所有读取的文档
     */
    default @NonNull List<Document> readDirectory(@NonNull String directory, boolean recursive) {
        Path dirPath = Path.of(directory);
        if (!Files.isDirectory(dirPath)) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        List<Source> sources = new ArrayList<>();
        try {
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;
            Files.walk(dirPath, maxDepth)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    Source source = Source.ofFile(path.toString());
                    if (supports(source)) {
                        sources.add(source);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to walk directory: " + directory, e);
        }

        return readAll(sources);
    }
}