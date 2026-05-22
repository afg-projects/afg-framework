package io.github.afgprojects.framework.ai.rag.loader;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.DocumentLoader;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * 组合文档加载器
 *
 * <p>根据文件类型自动选择合适的加载器。支持通过 SPI 扩展新的加载器。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class CompositeDocumentLoader implements DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(CompositeDocumentLoader.class);

    private final List<DocumentLoader> loaders;
    private final EncodingDetector encodingDetector;

    /**
     * 创建组合加载器，使用默认加载器列表
     */
    public CompositeDocumentLoader() {
        this(loadDefaultLoaders(), new DefaultEncodingDetector());
    }

    /**
     * 创建组合加载器，使用指定加载器列表
     */
    public CompositeDocumentLoader(List<DocumentLoader> loaders, EncodingDetector encodingDetector) {
        this.loaders = new ArrayList<>(loaders);
        this.encodingDetector = encodingDetector;

        // 确保至少有基本加载器
        if (this.loaders.isEmpty()) {
            this.loaders.add(new TextDocumentLoader(encodingDetector));
        }

        log.debug("CompositeDocumentLoader initialized with {} loaders", this.loaders.size());
    }

    @Override
    public @NonNull List<Document> load(@NonNull Source source) {
        DocumentLoader loader = selectLoader(source);
        if (loader == null) {
            throw new RuntimeException("No suitable loader found for: " + source.getPath());
        }

        log.debug("Loading {} with {}", source.getPath(), loader.getClass().getSimpleName());
        return loader.load(source);
    }

    @Override
    public boolean supports(@NonNull Source source) {
        return selectLoader(source) != null;
    }

    /**
     * 选择合适的加载器
     */
    private DocumentLoader selectLoader(Source source) {
        for (DocumentLoader loader : loaders) {
            if (loader.supports(source)) {
                return loader;
            }
        }
        return null;
    }

    /**
     * 批量加载文件
     */
    public @NonNull List<Document> loadBatch(@NonNull List<String> paths) {
        List<Document> allDocuments = new ArrayList<>();
        for (String path : paths) {
            List<Document> docs = load(Source.ofFile(path));
            allDocuments.addAll(docs);
        }
        return allDocuments;
    }

    /**
     * 加载目录下所有文件
     */
    public @NonNull List<Document> loadDirectory(@NonNull String directory) {
        return loadDirectory(directory, false);
    }

    /**
     * 加载目录下所有文件
     */
    public @NonNull List<Document> loadDirectory(@NonNull String directory, boolean recursive) {
        java.nio.file.Path dirPath = java.nio.file.Path.of(directory);
        if (!java.nio.file.Files.isDirectory(dirPath)) {
            throw new RuntimeException("Not a directory: " + directory);
        }

        List<Document> allDocuments = new ArrayList<>();
        try {
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;
            java.nio.file.Files.walk(dirPath, maxDepth)
                .filter(java.nio.file.Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Source source = Source.ofFile(path.toString());
                        if (supports(source)) {
                            List<Document> docs = load(source);
                            allDocuments.addAll(docs);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load file: {}", path, e);
                    }
                });
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to walk directory: " + directory, e);
        }

        return allDocuments;
    }

    /**
     * 加载默认加载器列表
     */
    private static List<DocumentLoader> loadDefaultLoaders() {
        List<DocumentLoader> loaders = new ArrayList<>();

        // 内置加载器
        loaders.add(new PdfDocumentLoader());
        loaders.add(new MarkdownDocumentLoader());
        loaders.add(new TextDocumentLoader());

        // SPI 扩展加载器
        ServiceLoader<DocumentLoader> serviceLoader = ServiceLoader.load(DocumentLoader.class);
        for (DocumentLoader loader : serviceLoader) {
            if (!(loader instanceof CompositeDocumentLoader)) {
                loaders.add(loader);
                log.debug("Loaded SPI loader: {}", loader.getClass().getName());
            }
        }

        return loaders;
    }

    /**
     * 注册新的加载器
     */
    public void registerLoader(DocumentLoader loader) {
        loaders.add(loader);
        log.debug("Registered loader: {}", loader.getClass().getName());
    }

    /**
     * 获取所有加载器
     */
    public List<DocumentLoader> getLoaders() {
        return new ArrayList<>(loaders);
    }
}