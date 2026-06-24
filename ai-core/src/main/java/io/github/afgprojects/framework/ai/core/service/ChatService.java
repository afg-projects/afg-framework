package io.github.afgprojects.framework.ai.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.ChatPipeline;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineResult;
import io.github.afgprojects.framework.ai.core.api.pipeline.TokenUsage;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.entity.application.ApplicationEntity;
import io.github.afgprojects.framework.ai.core.entity.chat.ChatLogEntity;
import io.github.afgprojects.framework.ai.core.pipeline.DefaultPipelineContext;
import io.github.afgprojects.framework.ai.core.pipeline.SimpleApplicationConfig;
import io.github.afgprojects.framework.ai.core.pipeline.SimpleConfig;
import io.github.afgprojects.framework.data.core.DataManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

/**
 * 对话服务
 *
 * <p>负责管理对话会话、执行对话流水线、保存对话日志。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final DataManager dataManager;
    private final ChatPipeline chatPipeline;
    private final SessionStore sessionStore;
    private final MessageHistoryStore messageHistoryStore;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    /**
     * 同步对话
     * <p>
     * 不使用 @Transactional：ChatPipeline.execute() 是 AI 调用，可能耗时较长。
     * 对话日志的保存在独立短事务中通过 TransactionTemplate 完成。
     *
     * @param conversationId 会话 ID
     * @param applicationId  应用 ID
     * @param message        用户消息
     * @param userId         用户 ID
     * @return 流水线执行结果
     */
    public PipelineResult chat(String conversationId, String applicationId, String message, String userId) {
        PipelineContext context = buildPipelineContext(conversationId, applicationId, message, userId);

        // AI 调用，不在事务中
        PipelineResult result = chatPipeline.execute(context);

        // 保存对话日志（短事务）
        saveChatLogInTransaction(applicationId, conversationId, message, result, userId);
        return result;
    }

    /**
     * 流式对话
     *
     * @param conversationId 会话 ID
     * @param applicationId  应用 ID
     * @param message        用户消息
     * @param userId         用户 ID
     * @return 流式响应
     */
    public Flux<String> chatStream(String conversationId, String applicationId, String message, String userId) {
        PipelineContext context = buildPipelineContext(conversationId, applicationId, message, userId);
        return chatPipeline.executeStream(context)
            .doOnComplete(() -> log.info("Stream chat completed: conversationId={}", conversationId))
            .doOnError(err -> {
                log.warn("Stream chat failed: conversationId={}, error={}", conversationId, err.getMessage());
                saveChatLogAsync(applicationId, conversationId, message, err.getMessage(), userId);
            });
    }

    /**
     * 构建流水线上下文
     */
    private PipelineContext buildPipelineContext(String conversationId, String applicationId,
                                                  String message, String userId) {
        ApplicationConfig config = loadApplicationConfig(applicationId);

        return DefaultPipelineContext.builder()
            .applicationId(applicationId)
            .config(config)
            .conversationId(conversationId)
            .userId(userId)
            .chatUserId(userId)
            .userMessage(message)
            .build();
    }

    /**
     * 加载应用配置
     *
     * <p>从数据库加载 ApplicationEntity，解析其 config JSON 字段构建 ApplicationConfig。
     * 如果应用不存在或配置为空，使用默认配置。
     */
    private ApplicationConfig loadApplicationConfig(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            return new SimpleApplicationConfig(new SimpleConfig());
        }

        try {
            return dataManager.findById(ApplicationEntity.class, applicationId)
                .map(entity -> parseApplicationConfig(entity.getConfig()))
                .orElseGet(() -> {
                    log.debug("Application not found: {}, using default config", applicationId);
                    return new SimpleApplicationConfig(new SimpleConfig());
                });
        } catch (NumberFormatException e) {
            log.debug("Invalid applicationId: {}, using default config", applicationId);
            return new SimpleApplicationConfig(new SimpleConfig());
        }
    }

    /**
     * 解析应用配置 JSON
     */
    private ApplicationConfig parseApplicationConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) {
            return new SimpleApplicationConfig(new SimpleConfig());
        }

        try {
            SimpleConfig config = objectMapper.readValue(configJson, SimpleConfig.class);
            return new SimpleApplicationConfig(config);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse application config JSON: {}", e.getMessage());
            return new SimpleApplicationConfig(new SimpleConfig());
        }
    }

    /**
     * 在独立短事务中保存对话日志
     */
    private void saveChatLogInTransaction(String applicationId, String conversationId, String question,
                                          PipelineResult result, String userId) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                ChatLogEntity logEntity = new ChatLogEntity();
                if (applicationId != null && !applicationId.isBlank()) {
                    logEntity.setApplicationId(applicationId);
                }
                logEntity.setSessionId(conversationId);
                logEntity.setUserId(userId);
                logEntity.setQuestion(question);
                logEntity.setAnswer(result.content());
                logEntity.setDurationMs(result.durationMs());

                if (result.tokenUsage() != null) {
                    TokenUsage usage = result.tokenUsage();
                    logEntity.setInputTokens(usage.promptTokens());
                    logEntity.setOutputTokens(usage.completionTokens());
                }

                dataManager.save(ChatLogEntity.class, logEntity);
                log.debug("Chat log saved: conversationId={}", conversationId);
            });
        } catch (Exception e) {
            log.warn("Failed to save chat log: {}", e.getMessage());
        }
    }

    /**
     * 异步保存对话日志（流式对话失败时使用）
     */
    private void saveChatLogAsync(String applicationId, String conversationId, String question,
                                   String errorMessage, String userId) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                ChatLogEntity logEntity = new ChatLogEntity();
                if (applicationId != null && !applicationId.isBlank()) {
                    logEntity.setApplicationId(applicationId);
                }
                logEntity.setSessionId(conversationId);
                logEntity.setUserId(userId);
                logEntity.setQuestion(question);
                logEntity.setAnswer(errorMessage);
                dataManager.save(ChatLogEntity.class, logEntity);
            });
        } catch (Exception e) {
            log.warn("Failed to save chat log (async): {}", e.getMessage());
        }
    }
}