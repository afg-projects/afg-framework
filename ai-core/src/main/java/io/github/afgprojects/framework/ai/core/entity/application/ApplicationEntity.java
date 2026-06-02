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
 * AI 应用实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_application")
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "access_token", length = 200)
    private String accessToken;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "config", columnDefinition = "JSON")
    private String config;

    @Column(name = "icon", length = 500)
    private String icon;

    @Column(name = "sort")
    private Integer sort = 0;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
