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
 * AI 文档实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_document")
public class DocumentEntity extends TenantEntity implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "knowledge_base_id", nullable = false)
    private Long knowledgeBaseId;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
}
