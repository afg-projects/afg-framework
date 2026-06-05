package io.github.afgprojects.framework.ai.langchain4j.advisor;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger;
import io.github.afgprojects.framework.ai.core.api.observability.AuditLogger.AuditStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * LangChain4J 审计日志 Advisor
 *
 * <p>将 LangChain4J 的 {@link ChatModelListener} 事件桥接到 AFG 的
 * {@link AuditLogger} 接口，实现统一的审计日志支持。
 *
 * <p>功能：
 * <ul>
 *   <li>onRequest：记录请求审计日志（状态 PENDING）</li>
 *   <li>onResponse：记录响应审计日志（状态 SUCCESS），包含 Token 使用量和延迟</li>
 *   <li>onError：记录错误审计日志（状态 FAILURE）</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class Lc4jAuditAdvisor implements ChatModelListener {

    private static final String OPERATION_TYPE = "chat";
    private static final String ATTR_START_TIME = "afg.audit.startTime";
    private static final String ATTR_MODEL_NAME = "afg.audit.modelName";
    private static final String ATTR_USER_MESSAGE = "afg.audit.userMessage";

    private final AuditLogger auditLogger;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ChatRequest chatRequest = requestContext.chatRequest();
        String modelName = extractModelName(chatRequest);
        String userMessage = extractLastUserMessage(chatRequest);

        // 存储到 attributes 中，供 onResponse/onError 使用
        Map<Object, Object> attributes = requestContext.attributes();
        attributes.put(ATTR_START_TIME, System.currentTimeMillis());
        attributes.put(ATTR_MODEL_NAME, modelName);
        attributes.put(ATTR_USER_MESSAGE, userMessage);

        // 记录请求审计日志
        auditLogger.log(
            null, // userId - 从上下文中无法直接获取
            OPERATION_TYPE,
            modelName,
            userMessage,
            null,
            AuditStatus.SUCCESS // 请求阶段标记为 SUCCESS，响应阶段会更新
        );

        log.debug("LC4J audit: request logged, modelName={}", modelName);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        Map<Object, Object> attributes = responseContext.attributes();
        Long startTime = (Long) attributes.get(ATTR_START_TIME);
        String modelName = (String) attributes.get(ATTR_MODEL_NAME);
        String userMessage = (String) attributes.get(ATTR_USER_MESSAGE);

        ChatResponse chatResponse = responseContext.chatResponse();
        String responseContent = extractResponseContent(chatResponse);

        // 计算响应时间
        Long responseTimeMs = null;
        if (startTime != null) {
            responseTimeMs = System.currentTimeMillis() - startTime;
        }

        // 记录响应审计日志
        auditLogger.log(
            null,
            OPERATION_TYPE,
            modelName != null ? modelName : "unknown",
            userMessage,
            responseContent,
            AuditStatus.SUCCESS
        );

        log.debug("LC4J audit: response logged, modelName={}, responseTimeMs={}", modelName, responseTimeMs);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Map<Object, Object> attributes = errorContext.attributes();
        Long startTime = (Long) attributes.get(ATTR_START_TIME);
        String modelName = (String) attributes.get(ATTR_MODEL_NAME);
        String userMessage = (String) attributes.get(ATTR_USER_MESSAGE);

        Throwable error = errorContext.error();
        String errorMessage = error != null ? error.getMessage() : "unknown error";

        // 计算响应时间
        Long responseTimeMs = null;
        if (startTime != null) {
            responseTimeMs = System.currentTimeMillis() - startTime;
        }

        // 记录错误审计日志
        auditLogger.log(
            null,
            OPERATION_TYPE,
            modelName != null ? modelName : "unknown",
            userMessage,
            errorMessage,
            AuditStatus.FAILURE
        );

        log.debug("LC4J audit: error logged, modelName={}, responseTimeMs={}", modelName, responseTimeMs);
    }

    /**
     * 从 ChatRequest 中提取模型名称
     */
    private String extractModelName(ChatRequest chatRequest) {
        if (chatRequest != null && chatRequest.parameters() != null) {
            String modelName = chatRequest.parameters().modelName();
            if (modelName != null && !modelName.isEmpty()) {
                return modelName;
            }
        }
        return "unknown";
    }

    /**
     * 从 ChatRequest 中提取最后一条用户消息
     */
    private String extractLastUserMessage(ChatRequest chatRequest) {
        if (chatRequest == null || chatRequest.messages() == null) {
            return null;
        }
        return chatRequest.messages().stream()
            .filter(msg -> msg instanceof UserMessage)
            .map(msg -> (UserMessage) msg)
            .reduce((first, second) -> second) // 取最后一条
            .map(UserMessage::singleText)
            .orElse(null);
    }

    /**
     * 从 ChatResponse 中提取响应内容
     */
    private String extractResponseContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.aiMessage() == null) {
            return null;
        }
        return chatResponse.aiMessage().text();
    }
}
