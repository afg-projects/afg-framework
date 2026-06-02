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
 * Document chunk entity for RAG system.
 *
 * <p>Represents a chunk of a document, with its embedding vector for similarity search.
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@AfEntity
@Table(name = "ai_document_chunk")
public class DocumentChunkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chunk_id", nullable = false, length = 64, unique = true)
    private String chunkId;

    @Column(name = "document_id", nullable = false, length = 64)
    private String documentId;

    @Column(name = "knowledge_base_id", nullable = false, length = 64)
    private String knowledgeBaseId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "embedding", columnDefinition = "TEXT")
    private String embedding;

    @Column(name = "score")
    private Double score;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
