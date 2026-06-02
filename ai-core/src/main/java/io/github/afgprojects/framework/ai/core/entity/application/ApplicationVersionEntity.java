package io.github.afgprojects.framework.ai.core.entity.application;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
@AfEntity
@Table(name = "ai_application_version")
public class ApplicationVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

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

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
