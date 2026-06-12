package io.github.afgprojects.framework.ai.core.service;

import io.github.afgprojects.framework.ai.core.api.rag.Document;
import io.github.afgprojects.framework.ai.core.api.rag.KnowledgeBaseService;
import io.github.afgprojects.framework.ai.core.entity.knowledge.DocumentEntity;
import io.github.afgprojects.framework.ai.core.entity.knowledge.KnowledgeBaseEntity;
import io.github.afgprojects.framework.commons.exception.BusinessException;
import io.github.afgprojects.framework.commons.exception.CommonErrorCode;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库扩展服务
 *
 * <p>提供 QA 问答、URL 上传、文档状态查询/重试功能。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeExtensionService {

    private final DataManager dataManager;
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 基于知识库的 QA 问答
     *
     * <p>基于 Embedding + VectorStore 检索知识库中最相关的文档片段，
     * 返回匹配结果供前端展示或进一步处理。
     *
     * @param knowledgeBaseId 知识库ID
     * @param question        用户问题
     * @param topK            返回结果数量
     * @param threshold       相似度阈值
     * @return 检索结果列表
     */
    public List<Document> questionAnswer(Long knowledgeBaseId, String question, int topK, double threshold) {
        // 验证知识库存在
        dataManager.findById(KnowledgeBaseEntity.class, knowledgeBaseId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "知识库不存在: " + knowledgeBaseId));

        // 使用 KnowledgeBaseService 进行向量检索
        return knowledgeBaseService.search(
            String.valueOf(knowledgeBaseId), question, topK, threshold);
    }

    /**
     * 通过 URL 上传文档到知识库
     *
     * <p>从 URL 下载内容，创建文档实体。
     * 目前支持纯文本 URL 内容，后续可扩展 PDF 等格式。
     *
     * @param knowledgeBaseId 知识库ID
     * @param url             文档 URL
     * @param title           文档标题（可选，默认使用 URL）
     * @return 创建的文档实体
     */
    @Transactional
    public DocumentEntity uploadFromUrl(Long knowledgeBaseId, String url, String title) {
        // 验证知识库存在
        dataManager.findById(KnowledgeBaseEntity.class, knowledgeBaseId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "知识库不存在: " + knowledgeBaseId));

        // 创建文档实体
        DocumentEntity document = new DocumentEntity();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title != null ? title : url);
        document.setSourceUrl(url);
        document.setStatus("PENDING");
        dataManager.save(DocumentEntity.class, document);

        log.info("Document created from URL: id={}, url={}", document.getId(), url);
        return document;
    }

    /**
     * 重试处理失败的文档
     *
     * <p>将文档状态重置为 PENDING，由后续处理流程重新执行。
     *
     * @param documentId 文档ID
     * @return 更新后的文档实体
     */
    @Transactional
    public DocumentEntity retryDocument(Long documentId) {
        DocumentEntity document = dataManager.findById(DocumentEntity.class, documentId)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.ENTITY_NOT_FOUND, "文档不存在: " + documentId));

        if (!"FAILED".equals(document.getStatus())) {
            throw new IllegalStateException("只能重试失败的文档，当前状态: " + document.getStatus());
        }

        document.setStatus("PENDING");
        document.setError(null);
        DocumentEntity updated = dataManager.save(DocumentEntity.class, document);

        log.info("Document retry: id={}", documentId);
        return updated;
    }

    /**
     * 查询知识库下指定状态的文档
     *
     * @param knowledgeBaseId 知识库ID
     * @param status          文档状态（可选）
     * @return 文档列表
     */
    public List<DocumentEntity> listDocumentsByStatus(Long knowledgeBaseId, String status) {
        var builder = Conditions.builder(DocumentEntity.class)
            .eq(DocumentEntity::getKnowledgeBaseId, knowledgeBaseId);

        if (status != null && !status.isEmpty()) {
            builder.eq(DocumentEntity::getStatus, status);
        }

        return dataManager.entity(DocumentEntity.class)
            .query()
            .where(builder.build())
            .orderByDesc(DocumentEntity::getCreatedAt)
            .list();
    }
}
