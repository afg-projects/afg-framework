package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.dto.knowledge.CreateKnowledgeBaseRequest;
import io.github.afgprojects.framework.ai.core.dto.knowledge.KnowledgeSearchRequest;
import io.github.afgprojects.framework.ai.core.dto.knowledge.UpdateKnowledgeBaseRequest;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentChunkEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.KnowledgeBaseEntity;
import io.github.afgprojects.framework.ai.core.service.KnowledgeDocumentService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 知识库管理控制器
 *
 * <p>提供知识库、文档、分块的 CRUD 和搜索接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class AiKnowledgeController {

    private final DataManager dataManager;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final KnowledgeBaseService knowledgeBaseService;

    // ==================== 知识库 CRUD ====================

    /**
     * 创建知识库
     */
    @PostMapping("/bases")
    @Transactional
    public KnowledgeBaseEntity createKnowledgeBase(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setEmbeddingModelName(request.getEmbeddingModelName());
        entity.setConfig(request.getConfig());
        return dataManager.save(KnowledgeBaseEntity.class, entity);
    }

    /**
     * 列出所有知识库
     */
    @GetMapping("/bases")
    public List<KnowledgeBaseEntity> listKnowledgeBases() {
        return dataManager.findAll(KnowledgeBaseEntity.class);
    }

    /**
     * 获取单个知识库
     */
    @GetMapping("/bases/{id}")
    public ResponseEntity<KnowledgeBaseEntity> getKnowledgeBase(@PathVariable Long id) {
        return dataManager.findById(KnowledgeBaseEntity.class, id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 更新知识库
     */
    @PutMapping("/bases/{id}")
    @Transactional
    public KnowledgeBaseEntity updateKnowledgeBase(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateKnowledgeBaseRequest request) {
        KnowledgeBaseEntity entity = dataManager.findById(KnowledgeBaseEntity.class, id)
            .orElseThrow(() -> new IllegalArgumentException("Knowledge base not found: " + id));

        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getEmbeddingModelName() != null) {
            entity.setEmbeddingModelName(request.getEmbeddingModelName());
        }
        if (request.getConfig() != null) {
            entity.setConfig(request.getConfig());
        }

        return dataManager.save(KnowledgeBaseEntity.class, entity);
    }

    /**
     * 删除知识库（软删除）
     */
    @DeleteMapping("/bases/{id}")
    @Transactional
    public ResponseEntity<Void> deleteKnowledgeBase(@PathVariable Long id) {
        KnowledgeBaseEntity entity = dataManager.findById(KnowledgeBaseEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }
        entity.markDeleted();
        dataManager.save(KnowledgeBaseEntity.class, entity);
        return ResponseEntity.noContent().build();
    }

    // ==================== 文档管理 ====================

    /**
     * 上传文档到知识库
     */
    @PostMapping("/bases/{id}/documents")
    @Transactional
    public DocumentEntity uploadDocument(@PathVariable Long id,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam(value = "title", required = false) String title) {
        return knowledgeDocumentService.uploadDocument(id, file, title);
    }

    /**
     * 列出知识库下的文档
     */
    @GetMapping("/bases/{id}/documents")
    public List<DocumentEntity> listDocuments(@PathVariable Long id) {
        return dataManager.entity(DocumentEntity.class)
            .query()
            .where(Conditions.builder(DocumentEntity.class)
                .eq(DocumentEntity::getKnowledgeBaseId, id)
                .build())
            .orderByDesc(DocumentEntity::getCreatedAt)
            .list();
    }

    /**
     * 获取单个文档
     */
    @GetMapping("/bases/{id}/documents/{docId}")
    public ResponseEntity<DocumentEntity> getDocument(@PathVariable Long id,
                                                       @PathVariable Long docId) {
        return dataManager.findById(DocumentEntity.class, docId)
            .filter(doc -> doc.getKnowledgeBaseId().equals(id))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除文档（软删除）
     */
    @DeleteMapping("/bases/{id}/documents/{docId}")
    @Transactional
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id,
                                                @PathVariable Long docId) {
        DocumentEntity doc = dataManager.findById(DocumentEntity.class, docId)
            .filter(d -> d.getKnowledgeBaseId().equals(id))
            .orElse(null);
        if (doc == null) {
            return ResponseEntity.notFound().build();
        }
        knowledgeDocumentService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 分块管理 ====================

    /**
     * 列出文档的分块
     */
    @GetMapping("/bases/{id}/documents/{docId}/chunks")
    public List<DocumentChunkEntity> listChunks(@PathVariable Long id,
                                                 @PathVariable Long docId) {
        return dataManager.entity(DocumentChunkEntity.class)
            .query()
            .where(Conditions.builder(DocumentChunkEntity.class)
                .eq(DocumentChunkEntity::getDocumentId, docId)
                .eq(DocumentChunkEntity::getKnowledgeBaseId, id)
                .build())
            .orderByAsc(DocumentChunkEntity::getChunkIndex)
            .list();
    }

    /**
     * 删除分块
     */
    @DeleteMapping("/chunks/{chunkId}")
    @Transactional
    public ResponseEntity<Void> deleteChunk(@PathVariable Long chunkId) {
        if (!dataManager.existsById(DocumentChunkEntity.class, chunkId)) {
            return ResponseEntity.notFound().build();
        }
        knowledgeDocumentService.deleteChunk(chunkId);
        return ResponseEntity.noContent().build();
    }

    // ==================== 知识库搜索 ====================

    /**
     * 搜索知识库
     */
    @PostMapping("/search")
    public List<Document> searchKnowledge(@Valid @RequestBody KnowledgeSearchRequest request) {
        List<Document> allResults = new ArrayList<>();
        for (String kbId : request.getKnowledgeBaseIds()) {
            List<Document> results = knowledgeBaseService.search(
                kbId, request.getQuery(), request.getTopK(), request.getSimilarityThreshold());
            allResults.addAll(results);
        }
        return allResults;
    }
}
