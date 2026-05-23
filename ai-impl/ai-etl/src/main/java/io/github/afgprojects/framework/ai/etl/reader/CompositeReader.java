package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.etl.Document;
import io.github.afgprojects.framework.ai.core.etl.DocumentReader;
import io.github.afgprojects.framework.ai.core.etl.Source;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 组合文档读取器。
 *
 * <p>根据文件类型自动选择合适的读取器。支持通过 SPI 扩展新的读取器。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class CompositeReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(CompositeReader.class);

    private final List<DocumentReader> readers;

    /**
     * 创建组合读取器，使用默认读取器列表。
     */
    public CompositeReader() {
        this(List.of(
            new PdfReader(),
            new MarkdownReader(),
            new TextReader()
        ));
    }

    /**
     * 创建组合读取器，使用指定读取器列表。
     *
     * @param readers 读取器列表
     */
    public CompositeReader(List<DocumentReader> readers) {
        this.readers = new ArrayList<>(readers);

        // 确保至少有基本读取器
        if (this.readers.isEmpty()) {
            this.readers.add(new TextReader());
        }

        log.debug("CompositeReader initialized with {} readers", this.readers.size());
    }

    @Override
    public @NonNull List<Document> read(@NonNull Source source) {
        DocumentReader reader = selectReader(source);
        if (reader == null) {
            throw new RuntimeException("No suitable reader found for: " + source.getPath());
        }

        log.debug("Reading {} with {}", source.getPath(), reader.getClass().getSimpleName());
        return reader.read(source);
    }

    @Override
    public boolean supports(@NonNull Source source) {
        return selectReader(source) != null;
    }

    /**
     * 注册新的读取器。
     *
     * @param reader 读取器实例
     */
    public void registerReader(@NonNull DocumentReader reader) {
        readers.add(reader);
        log.debug("Registered reader: {}", reader.getClass().getName());
    }

    /**
     * 获取所有读取器。
     *
     * @return 读取器列表
     */
    @NonNull
    public List<DocumentReader> getReaders() {
        return new ArrayList<>(readers);
    }

    /**
     * 选择合适的读取器。
     */
    private DocumentReader selectReader(Source source) {
        for (DocumentReader reader : readers) {
            if (reader.supports(source)) {
                return reader;
            }
        }
        return null;
    }
}