package io.github.afgprojects.framework.ai.core.entity.application;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 应用版本实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_application_version")
public class ApplicationVersionEntity extends SoftDeleteEntity {

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "version", nullable = false, length = 50)
    private String version;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "user_id", length = 64)
    private String userId;
}
