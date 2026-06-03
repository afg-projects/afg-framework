package io.github.afgprojects.framework.ai.core.entity.model;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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
public class ModelUsageEntity extends BaseEntity {

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
}
