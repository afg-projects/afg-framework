package io.github.afgprojects.framework.ai.core.entity.chat;

import io.github.afgprojects.framework.apt.entity.AfEntity;
import io.github.afgprojects.framework.data.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * AI 对话日志实体
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
@AfEntity
@Table(name = "ai_chat_log")
public class ChatLogEntity extends BaseEntity {

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "session_id", length = 64)
    private String sessionId;

    @Column(name = "user_id", length = 64)
    private String userId;

    @Column(name = "question", columnDefinition = "TEXT")
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT")
    private String answer;

    @Column(name = "input_tokens")
    private Long inputTokens;

    @Column(name = "output_tokens")
    private Long outputTokens;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "model_config_id")
    private Long modelConfigId;

    @Column(name = "vote")
    private Integer vote;

    @Column(name = "vote_reason", length = 500)
    private String voteReason;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;
}
