package io.github.afgprojects.framework.ai.core.controller;

import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineResult;
import io.github.afgprojects.framework.ai.core.api.persistence.MessageHistoryStore;
import io.github.afgprojects.framework.ai.core.api.persistence.SessionStore;
import io.github.afgprojects.framework.ai.core.dto.chat.ChatLogQuery;
import io.github.afgprojects.framework.ai.core.dto.chat.ChatRequest;
import io.github.afgprojects.framework.ai.core.dto.chat.CreateConversationRequest;
import io.github.afgprojects.framework.ai.core.entity.chat.ChatLogEntity;
import io.github.afgprojects.framework.ai.core.service.ChatService;
import io.github.afgprojects.framework.data.core.DataManager;
import io.github.afgprojects.framework.data.core.condition.Conditions;
import io.github.afgprojects.framework.data.core.condition.TypedConditionBuilder;
import io.github.afgprojects.framework.data.core.query.Condition;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话控制器
 *
 * <p>提供对话会话管理、消息发送（同步/流式）、消息历史、对话日志查询等接口。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final DataManager dataManager;
    private final ChatService chatService;
    private final SessionStore sessionStore;
    private final MessageHistoryStore messageHistoryStore;

    // ==================== 对话会话管理 ====================

    /**
     * 创建对话会话
     */
    @PostMapping("/conversations")
    @Transactional
    public Map<String, Object> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        String userId = getCurrentUserId();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("applicationId", request.getApplicationId());
        if (request.getTitle() != null) {
            metadata.put("title", request.getTitle());
        }

        SessionStore.SessionContext context = createSessionContext(userId, metadata);
        SessionStore.Session session = sessionStore.createSession(context);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", session.getSessionId());
        result.put("userId", session.getUserId());
        result.put("state", session.getState().name());
        result.put("createdAt", session.getCreatedAt());
        result.put("metadata", session.getMetadata());
        return result;
    }

    /**
     * 列出对话会话（可选按 userId 筛选）
     */
    @GetMapping("/conversations")
    public List<SessionStore.Session> listConversations(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "50") int limit) {
        String effectiveUserId = userId != null ? userId : getCurrentUserId();
        return sessionStore.getActiveSessions(effectiveUserId, limit);
    }

    /**
     * 获取单个对话会话
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<SessionStore.Session> getConversation(@PathVariable String id) {
        SessionStore.Session session = sessionStore.getSession(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    /**
     * 删除对话会话
     */
    @DeleteMapping("/conversations/{id}")
    @Transactional
    public ResponseEntity<Void> deleteConversation(@PathVariable String id) {
        SessionStore.Session session = sessionStore.getSession(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        messageHistoryStore.deleteSessionMessages(id);
        sessionStore.deleteSession(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== 消息 ====================

    /**
     * 发送消息
     *
     * <p>如果 request.stream=true，返回 SSE 流式响应；否则同步返回执行结果。
     */
    @PostMapping("/conversations/{id}/messages")
    public Object sendMessage(@PathVariable String id,
                               @Valid @RequestBody ChatRequest request) {
        String userId = getCurrentUserId();
        SessionStore.Session session = sessionStore.getSession(id);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        String applicationId = session.getMetadata().get("applicationId");

        if (request.isStream()) {
            return chatService.chatStream(id, applicationId, request.getMessage(), userId);
        } else {
            return chatService.chat(id, applicationId, request.getMessage(), userId);
        }
    }

    /**
     * 流式对话（SSE）
     */
    @GetMapping(value = "/conversations/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> sendMessageStream(@PathVariable String id,
                                           @RequestParam String message) {
        String userId = getCurrentUserId();
        SessionStore.Session session = sessionStore.getSession(id);
        if (session == null) {
            return Flux.empty();
        }

        String applicationId = session.getMetadata().get("applicationId");
        return chatService.chatStream(id, applicationId, message, userId);
    }

    /**
     * 获取消息历史
     */
    @GetMapping("/conversations/{id}/messages")
    public List<MessageHistoryStore.Message> getMessageHistory(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "50") int limit) {
        return messageHistoryStore.getMessages(id, offset, limit);
    }

    // ==================== 对话日志 ====================

    /**
     * 查询对话日志（分页，可按 applicationId/sessionId/userId 筛选）
     */
    @GetMapping("/logs")
    public List<ChatLogEntity> queryChatLogs(ChatLogQuery query) {
        TypedConditionBuilder<ChatLogEntity> builder = Conditions.builder(ChatLogEntity.class);

        if (query.getApplicationId() != null) {
            builder.eq(ChatLogEntity::getApplicationId, query.getApplicationId());
        }
        if (query.getSessionId() != null) {
            builder.eq(ChatLogEntity::getSessionId, query.getSessionId());
        }
        if (query.getUserId() != null) {
            builder.eq(ChatLogEntity::getUserId, query.getUserId());
        }

        Condition condition = builder.build();
        int offset = (query.getPage() - 1) * query.getSize();

        return dataManager.entity(ChatLogEntity.class)
            .query()
            .where(condition)
            .orderByDesc(ChatLogEntity::getCreatedAt)
            .offset(offset)
            .limit(query.getSize())
            .list();
    }

    /**
     * 对对话日志投票
     */
    @PutMapping("/logs/{id}/vote")
    @Transactional
    public ResponseEntity<ChatLogEntity> voteChatLog(@PathVariable Long id,
                                                      @RequestBody Map<String, Object> voteRequest) {
        ChatLogEntity entity = dataManager.findById(ChatLogEntity.class, id)
            .orElse(null);
        if (entity == null) {
            return ResponseEntity.notFound().build();
        }

        Object vote = voteRequest.get("vote");
        if (vote instanceof Number number) {
            entity.setVote(number.intValue());
        }

        Object reason = voteRequest.get("reason");
        if (reason instanceof String s) {
            entity.setVoteReason(s);
        }

        ChatLogEntity saved = dataManager.save(ChatLogEntity.class, entity);
        return ResponseEntity.ok(saved);
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建会话上下文
     */
    private SessionStore.SessionContext createSessionContext(String userId, Map<String, String> metadata) {
        return new SessionStore.SessionContext() {
            @Override
            public String getUserId() { return userId; }

            @Override
            public String getTenantId() { return null; }

            @Override
            public String getModelName() { return null; }

            @Override
            public String getSystemPrompt() { return null; }

            @Override
            public Long getExpiresInSeconds() { return null; }

            @Override
            public Map<String, String> getMetadata() { return metadata; }
        };
    }

    /**
     * 获取当前用户 ID
     *
     * <p>TODO: 从 SecurityContext 获取实际用户 ID
     */
    private String getCurrentUserId() {
        return "system";
    }
}
