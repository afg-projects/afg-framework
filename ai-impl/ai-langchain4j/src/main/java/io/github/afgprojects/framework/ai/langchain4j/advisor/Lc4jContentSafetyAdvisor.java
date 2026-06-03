package io.github.afgprojects.framework.ai.langchain4j.advisor;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import io.github.afgprojects.framework.ai.core.api.exception.AiException;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker.SafetyCheckContext;
import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker.SafetyCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.Collections;
import java.util.Map;

/**
 * LangChain4J 内容安全 Advisor
 *
 * <p>将 LangChain4J 的 {@link ChatModelListener} 事件桥接到 AFG 的
 * {@link ContentSafetyChecker} 接口，实现统一的输入内容安全检查。
 *
 * <p>功能：
 * <ul>
 *   <li>onRequest：提取最后一条用户消息，调用 ContentSafetyChecker.checkInput()，不安全则抛 AiException</li>
 *   <li>onResponse：空实现</li>
 *   <li>onError：空实现</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class Lc4jContentSafetyAdvisor implements ChatModelListener {

    private static final String OPERATION_TYPE = "chat";
    private static final String ERROR_CODE_CONTENT_BLOCKED = "SAFETY_001";

    private final ContentSafetyChecker contentSafetyChecker;

    @Override
    public void onRequest(@NonNull ChatModelRequestContext requestContext) {
        ChatRequest chatRequest = requestContext.chatRequest();
        String userMessage = extractLastUserMessage(chatRequest);
        String modelName = extractModelName(chatRequest);

        if (userMessage == null || userMessage.isEmpty()) {
            log.debug("LC4J content safety: no user message found, skipping check");
            return;
        }

        // 构建检查上下文
        SafetyCheckContext context = new Lc4jSafetyCheckContext(null, null, modelName, OPERATION_TYPE);

        // 执行输入内容安全检查
        SafetyCheckResult result = contentSafetyChecker.checkInput(userMessage, context);

        if (result.isBlocked()) {
            log.warn("LC4J content safety: request blocked, riskLevel={}, reason={}",
                result.getRiskLevel(), result.getReason());
            throw new AiException(
                "Content safety check failed: " + result.getReason(),
                ERROR_CODE_CONTENT_BLOCKED
            );
        }

        log.debug("LC4J content safety: request allowed, riskLevel={}", result.getRiskLevel());
    }

    @Override
    public void onResponse(@NonNull ChatModelResponseContext responseContext) {
        // 内容安全检查在请求阶段完成，响应阶段不需要处理
    }

    @Override
    public void onError(@NonNull ChatModelErrorContext errorContext) {
        // 内容安全检查在请求阶段完成，错误阶段不需要处理
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
            .reduce((first, second) -> second)
            .map(UserMessage::singleText)
            .orElse(null);
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
     * LangChain4J 场景下的 SafetyCheckContext 实现
     */
    private record Lc4jSafetyCheckContext(
        String userId,
        String tenantId,
        String modelName,
        String operationType
    ) implements SafetyCheckContext {

        @Override
        public boolean isStrictMode() {
            return false;
        }

        @Override
        public @NonNull java.util.List<String> getCheckCategories() {
            return Collections.emptyList();
        }
    }
}
