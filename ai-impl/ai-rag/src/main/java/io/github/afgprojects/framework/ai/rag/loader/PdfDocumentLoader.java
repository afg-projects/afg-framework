package io.github.afgprojects.framework.ai.rag.loader;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.rag.DocumentLoader;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PDF 文档加载器
 *
 * <p>基于 Apache PDFBox 实现，支持按页分割、元数据提取。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class PdfDocumentLoader implements DocumentLoader {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentLoader.class);

    private final PdfLoadOptions options;

    public PdfDocumentLoader() {
        this(PdfLoadOptions.defaults());
    }

    public PdfDocumentLoader(PdfLoadOptions options) {
        this.options = options;
    }

    @Override
    public @NonNull List<Document> load(@NonNull Source source) {
        File file = new File(source.getPath());

        if (!file.exists()) {
            throw new RuntimeException("PDF file not found: " + source.getPath());
        }

        try (PDDocument pdfDoc = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalPages = pdfDoc.getNumberOfPages();
            int startPage = Math.max(1, options.startPage());
            int endPage = Math.min(totalPages, options.endPage());

            log.debug("Loading PDF: {} pages {}-{} of {}", source.getPath(), startPage, endPage, totalPages);

            if (options.splitByPage()) {
                return loadByPage(pdfDoc, stripper, startPage, endPage, totalPages, source.getPath());
            } else {
                return loadAsSingle(pdfDoc, stripper, startPage, endPage, totalPages, source.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load PDF: " + source.getPath(), e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".pdf") ||
               source.getContentType() != null && source.getContentType().equals("application/pdf");
    }

    /**
     * 按页加载 PDF
     */
    private List<Document> loadByPage(PDDocument pdfDoc, PDFTextStripper stripper,
                                       int startPage, int endPage, int totalPages, String filePath) throws IOException {
        List<Document> documents = new ArrayList<>();

        for (int page = startPage; page <= endPage; page++) {
            stripper.setStartPage(page);
            stripper.setEndPage(page);

            String text = stripper.getText(pdfDoc).trim();
            if (!text.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", filePath);
                metadata.put("contentType", "application/pdf");
                metadata.put("page", page);
                metadata.put("totalPages", totalPages);

                if (options.extractMetadata()) {
                    addPdfMetadata(pdfDoc, metadata);
                }

                Document document = new Document(
                    UUID.randomUUID().toString(),
                    text,
                    null,
                    metadata
                );
                documents.add(document);
            }
        }

        return documents;
    }

    /**
     * 作为单个文档加载 PDF
     */
    private List<Document> loadAsSingle(PDDocument pdfDoc, PDFTextStripper stripper,
                                         int startPage, int endPage, int totalPages, String filePath) throws IOException {
        stripper.setStartPage(startPage);
        stripper.setEndPage(endPage);

        String text = stripper.getText(pdfDoc).trim();
        if (text.isEmpty()) {
            return List.of();
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", filePath);
        metadata.put("contentType", "application/pdf");
        metadata.put("startPage", startPage);
        metadata.put("endPage", endPage);
        metadata.put("totalPages", totalPages);

        if (options.extractMetadata()) {
            addPdfMetadata(pdfDoc, metadata);
        }

        Document document = new Document(
            UUID.randomUUID().toString(),
            text,
            null,
            metadata
        );

        return List.of(document);
    }

    /**
     * 添加 PDF 元数据
     */
    private void addPdfMetadata(PDDocument pdfDoc, Map<String, Object> metadata) {
        PDDocumentInformation info = pdfDoc.getDocumentInformation();
        if (info != null) {
            if (info.getTitle() != null) {
                metadata.put("title", info.getTitle());
            }
            if (info.getAuthor() != null) {
                metadata.put("author", info.getAuthor());
            }
            if (info.getCreator() != null) {
                metadata.put("creator", info.getCreator());
            }
            if (info.getProducer() != null) {
                metadata.put("producer", info.getProducer());
            }
            if (info.getSubject() != null) {
                metadata.put("subject", info.getSubject());
            }
            if (info.getKeywords() != null) {
                metadata.put("keywords", info.getKeywords());
            }
        }
    }

    /**
     * PDF 加载选项
     */
    public record PdfLoadOptions(
        boolean splitByPage,
        int startPage,
        int endPage,
        boolean extractMetadata
    ) {
        /**
         * 默认选项：不按页分割，提取元数据
         */
        public static PdfLoadOptions defaults() {
            return new PdfLoadOptions(false, 1, Integer.MAX_VALUE, true);
        }

        /**
         * 按页分割选项
         */
        public static PdfLoadOptions byPage() {
            return new PdfLoadOptions(true, 1, Integer.MAX_VALUE, true);
        }

        /**
         * 指定页范围
         */
        public static PdfLoadOptions pages(int start, int end) {
            return new PdfLoadOptions(true, start, end, true);
        }
    }
}