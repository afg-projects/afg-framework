package io.github.afgprojects.framework.ai.etl.reader;

import io.github.afgprojects.framework.ai.core.rag.Document;
import io.github.afgprojects.framework.ai.core.etl.Source;
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
 * PDF 文档读取器。
 *
 * <p>基于 Apache PDFBox 实现，支持按页分割和元数据提取。
 *
 * @author AFG Projects
 * @since 1.0.0
 */
public class PdfReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(PdfReader.class);

    private final PdfReadOptions options;

    /**
     * 创建默认配置的 PDF 读取器。
     */
    public PdfReader() {
        this(PdfReadOptions.defaults());
    }

    /**
     * 创建带指定选项的 PDF 读取器。
     *
     * @param options 读取选项
     */
    public PdfReader(PdfReadOptions options) {
        this.options = options;
    }

    @Override
    public @NonNull List<Document> read(@NonNull Source source) {
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

            log.debug("Reading PDF: {} pages {}-{} of {}", source.getPath(), startPage, endPage, totalPages);

            if (options.splitByPage()) {
                return readByPage(pdfDoc, stripper, startPage, endPage, totalPages, source.getPath());
            } else {
                return readAsSingle(pdfDoc, stripper, startPage, endPage, totalPages, source.getPath());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read PDF: " + source.getPath(), e);
        }
    }

    @Override
    public boolean supports(@NonNull Source source) {
        String path = source.getPath().toLowerCase();
        return path.endsWith(".pdf") ||
               "application/pdf".equals(source.getContentType());
    }

    /**
     * 按页读取 PDF。
     */
    private List<Document> readByPage(PDDocument pdfDoc, PDFTextStripper stripper,
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
     * 作为单个文档读取 PDF。
     */
    private List<Document> readAsSingle(PDDocument pdfDoc, PDFTextStripper stripper,
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
     * 添加 PDF 元数据。
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
     * PDF 读取选项。
     *
     * @param splitByPage     是否按页分割
     * @param startPage       起始页码（从 1 开始）
     * @param endPage         结束页码
     * @param extractMetadata 是否提取 PDF 元数据
     */
    public record PdfReadOptions(
        boolean splitByPage,
        int startPage,
        int endPage,
        boolean extractMetadata
    ) {
        /**
         * 默认选项：不按页分割，提取元数据。
         */
        public static PdfReadOptions defaults() {
            return new PdfReadOptions(false, 1, Integer.MAX_VALUE, true);
        }

        /**
         * 按页分割选项。
         */
        public static PdfReadOptions byPage() {
            return new PdfReadOptions(true, 1, Integer.MAX_VALUE, true);
        }

        /**
         * 指定页范围选项。
         *
         * @param start 起始页码
         * @param end   结束页码
         * @return 选项实例
         */
        public static PdfReadOptions pages(int start, int end) {
            return new PdfReadOptions(true, start, end, true);
        }
    }
}