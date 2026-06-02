package io.github.afgprojects.framework.ai.rag.entity;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Document entity for RAG system.
 *
 * <p>Represents a document within a knowledge base.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_document")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false, length = 64, unique = true)
    private String documentId;

    @Column(name = "knowledge_base_id", nullable = false, length = 64)
    private String knowledgeBaseId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source", length = 500)
    private String source;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "md5", length = 32)
    private String md5;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
