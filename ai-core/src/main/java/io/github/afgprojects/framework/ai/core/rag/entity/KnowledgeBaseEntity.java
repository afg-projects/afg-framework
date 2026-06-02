package io.github.afgprojects.framework.ai.core.rag.entity;

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
 * Knowledge base entity for RAG system.
 *
 * <p>Represents a knowledge base that groups related documents together.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_knowledge_base")
public class KnowledgeBaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "knowledge_base_id", nullable = false, length = 64, unique = true)
    private String knowledgeBaseId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
