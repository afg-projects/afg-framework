package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.EmbeddingService;
import io.github.afgprojects.framework.ai.core.api.rag.VectorStore;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentChunkEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.KnowledgeBaseEntity;
import io.github.afgprojects.framework.ai.core.etl.transformer.RecursiveCharacterTextSplitter;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档服务
 *
 * <p>负责文档上传、文本提取、分块、向量化存储等完整流程。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {

    private final DataManager dataManager;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final PlatformTransactionManager transactionManager;

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_CHUNK_OVERLAP = 50;

    /**
     * 上传文档到知识库
     * <p>
     * 不使用 @Transactional：文本提取、分块、嵌入向量生成是耗时操作（网络调用），
     * 不应持有数据库事务。数据库操作在独立短事务中通过 TransactionTemplate 完成。
     *
     * @param knowledgeBaseId 知识库ID
     * @param file            上传的文件
     * @param title           文档标题
     * @return 保存后的文档实体
     */
    public DocumentEntity uploadDocument(Long knowledgeBaseId, MultipartFile file, String title) {
        // 1. 验证知识库是否存在
        KnowledgeBaseEntity kb = dataManager.findById(KnowledgeBaseEntity.class, knowledgeBaseId)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + knowledgeBaseId));

        // 2. 创建文档实体，状态为 PROCESSING（短事务）
        DocumentEntity document = createDocumentInTransaction(knowledgeBaseId, file, title);

        try {
            // 3. 提取文本（不在事务中）
            String text = extractText(file);

            // 4. 分割文本（不在事务中）
            RecursiveCharacterTextSplitter splitter = new RecursiveCharacterTextSplitter(
                DEFAULT_CHUNK_SIZE, DEFAULT_CHUNK_OVERLAP);
            List<String> chunks = splitter.split(text);

            // 5. 分块存储到 VectorStore + 保存 DocumentChunkEntity
            //    嵌入向量生成是网络调用，不在事务中
            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);

                // 构建向量文档元数据
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("knowledgeBaseId", String.valueOf(knowledgeBaseId));
                metadata.put("documentId", String.valueOf(document.getId()));
                metadata.put("chunkIndex", i);
                metadata.put("title", document.getTitle());

                // 生成嵌入向量（网络调用）
                List<Double> embedding = embeddingService.embed(chunkContent);

                // 存储到 VectorStore
                String vectorDocId = "doc-" + document.getId() + "-chunk-" + i;
                Document vectorDoc = Document.of(vectorDocId, chunkContent, embedding, metadata);
                vectorStore.add(vectorDoc);

                // 保存分块实体（短事务）
                saveChunkInTransaction(document.getId(), knowledgeBaseId, chunkContent, i);
            }

            // 6. 更新文档状态为 COMPLETED + 更新知识库计数（短事务）
            completeDocumentInTransaction(document.getId(), chunks.size(), knowledgeBaseId);

            log.info("Document uploaded successfully: id={}, chunks={}", document.getId(), chunks.size());

            // 重新加载文档以返回最新状态
            return dataManager.findById(DocumentEntity.class, document.getId()).orElse(document);

        } catch (Exception e) {
            log.error("Failed to process document: id={}", document.getId(), e);
            failDocumentInTransaction(document.getId(), e.getMessage());
            throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
        }
    }

    /**
     * 在独立短事务中创建文档实体
     */
    private DocumentEntity createDocumentInTransaction(Long knowledgeBaseId, MultipartFile file, String title) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            DocumentEntity document = new DocumentEntity();
            document.setKnowledgeBaseId(knowledgeBaseId);
            document.setTitle(title != null ? title : file.getOriginalFilename());
            document.setFileType(extractFileType(file.getOriginalFilename()));
            document.setFileSize(file.getSize());
            document.setStatus("PROCESSING");
            return dataManager.save(DocumentEntity.class, document);
        });
    }

    /**
     * 在独立短事务中保存分块实体
     */
    private void saveChunkInTransaction(Long documentId, Long knowledgeBaseId, String content, int chunkIndex) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            DocumentChunkEntity chunkEntity = new DocumentChunkEntity();
            chunkEntity.setDocumentId(documentId);
            chunkEntity.setKnowledgeBaseId(knowledgeBaseId);
            chunkEntity.setContent(content);
            chunkEntity.setChunkIndex(chunkIndex);
            dataManager.save(DocumentChunkEntity.class, chunkEntity);
        });
    }

    /**
     * 在独立短事务中更新文档状态为 COMPLETED 并更新知识库计数
     */
    private void completeDocumentInTransaction(Long documentId, int chunkCount, Long knowledgeBaseId) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            dataManager.findById(DocumentEntity.class, documentId).ifPresent(doc -> {
                doc.setStatus("COMPLETED");
                doc.setChunkCount(chunkCount);
                dataManager.save(DocumentEntity.class, doc);
            });
            dataManager.findById(KnowledgeBaseEntity.class, knowledgeBaseId).ifPresent(kb -> {
                kb.setDocumentCount(kb.getDocumentCount() + 1);
                kb.setChunkCount(kb.getChunkCount() + chunkCount);
                dataManager.save(KnowledgeBaseEntity.class, kb);
            });
        });
    }

    /**
     * 在独立短事务中更新文档状态为 FAILED
     */
    private void failDocumentInTransaction(Long documentId, String errorMessage) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                dataManager.findById(DocumentEntity.class, documentId).ifPresent(doc -> {
                    doc.setStatus("FAILED");
                    doc.setError(errorMessage);
                    dataManager.save(DocumentEntity.class, doc);
                });
            });
        } catch (Exception e) {
            log.warn("Failed to update document status to FAILED: {}", e.getMessage());
        }
    }

    /**
     * 删除文档及其所有分块
     *
     * @param documentId 文档ID
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        DocumentEntity document = dataManager.findById(DocumentEntity.class, documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // 软删除文档
        document.markDeleted();
        dataManager.save(DocumentEntity.class, document);

        // 删除 VectorStore 中的分块
        List<DocumentChunkEntity> chunks = dataManager.entity(DocumentChunkEntity.class)
            .query()
            .where(Conditions.builder(DocumentChunkEntity.class)
                .eq(DocumentChunkEntity::getDocumentId, documentId)
                .build())
            .list();

        for (DocumentChunkEntity chunk : chunks) {
            String vectorDocId = "doc-" + documentId + "-chunk-" + chunk.getChunkIndex();
            try {
                vectorStore.delete(vectorDocId);
            } catch (Exception e) {
                log.warn("Failed to delete vector document: {}", vectorDocId, e);
            }
            dataManager.deleteById(DocumentChunkEntity.class, chunk.getId());
        }

        // 更新知识库计数
        Long kbId = document.getKnowledgeBaseId();
        dataManager.findById(KnowledgeBaseEntity.class, kbId).ifPresent(kb -> {
            kb.setDocumentCount(Math.max(0, kb.getDocumentCount() - 1));
            kb.setChunkCount(Math.max(0, kb.getChunkCount() - chunks.size()));
            dataManager.save(KnowledgeBaseEntity.class, kb);
        });

        log.info("Document deleted: id={}, chunksRemoved={}", documentId, chunks.size());
    }

    /**
     * 删除分块
     *
     * @param chunkId 分块ID
     */
    @Transactional
    public void deleteChunk(Long chunkId) {
        DocumentChunkEntity chunk = dataManager.findById(DocumentChunkEntity.class, chunkId)
            .orElseThrow(() -> new IllegalArgumentException("Chunk not found: " + chunkId));

        // 删除 VectorStore 中的向量
        String vectorDocId = "doc-" + chunk.getDocumentId() + "-chunk-" + chunk.getChunkIndex();
        try {
            vectorStore.delete(vectorDocId);
        } catch (Exception e) {
            log.warn("Failed to delete vector document: {}", vectorDocId, e);
        }

        // 删除分块实体
        dataManager.deleteById(DocumentChunkEntity.class, chunkId);

        // 更新文档和知识库计数
        dataManager.findById(DocumentEntity.class, chunk.getDocumentId()).ifPresent(doc -> {
            doc.setChunkCount(Math.max(0, doc.getChunkCount() - 1));
            dataManager.save(DocumentEntity.class, doc);
        });

        dataManager.findById(KnowledgeBaseEntity.class, chunk.getKnowledgeBaseId()).ifPresent(kb -> {
            kb.setChunkCount(Math.max(0, kb.getChunkCount() - 1));
            dataManager.save(KnowledgeBaseEntity.class, kb);
        });

        log.info("Chunk deleted: id={}", chunkId);
    }

    private String extractText(MultipartFile file) throws IOException {
        // 目前支持纯文本和 Markdown 文件，后续可扩展 PDF 等格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null &&
            (originalFilename.toLowerCase().endsWith(".txt") ||
             originalFilename.toLowerCase().endsWith(".md") ||
             originalFilename.toLowerCase().endsWith(".markdown"))) {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        // 对于其他格式，尝试以 UTF-8 读取
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}