package io.github.afgprojects.framework.ai.core.entity.knowledge;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeletable;
import io.github.afgprojects.framework.data.core.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 知识库实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_knowledge_base")
public class KnowledgeBaseEntity extends TenantEntity implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "embedding_model_name", length = 200)
    private String embeddingModelName;

    @Column(name = "document_count")
    private Integer documentCount = 0;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "user_id", length = 64)
    private String userId;
}
