package io.github.afgprojects.framework.ai.core.entity.model;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型使用记录实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@AfEntity
@Table(name = "ai_model_usage")
public class ModelUsageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "model_config_id", nullable = false)
    private Long modelConfigId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "total_tokens")
    private Long totalTokens;

    @Column(name = "cost", precision = 19, scale = 6)
    private BigDecimal cost;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
