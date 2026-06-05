package io.github.afgprojects.framework.ai.langchain4j.advisor;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiContext;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiDetectionResult;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.PiiType;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.MaskingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * LangChain4J PII 检测 Advisor
 *
 * <p>将 LangChain4J 的 {@link ChatModelListener} 事件桥接到 AFG 的
 * {@link PiiDetector} 接口，实现统一的 PII 检测支持。
 *
 * <p>功能：
 * <ul>
 *   <li>onRequest：检测请求中的 PII，将结果放入 requestContext.attributes()</li>
 *   <li>onResponse：检测响应中的 PII，将结果放入 responseContext.attributes()</li>
 *   <li>onError：空实现</li>
 * </ul>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class Lc4jPiiAdvisor implements ChatModelListener {

    private static final String ATTR_REQUEST_PII_RESULT = "afg.pii.requestResult";
    private static final String ATTR_RESPONSE_PII_RESULT = "afg.pii.responseResult";
    private static final String OPERATION_TYPE = "chat";

    private final PiiDetector piiDetector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ChatRequest chatRequest = requestContext.chatRequest();
        String userMessage = extractLastUserMessage(chatRequest);
        String modelName = extractModelName(chatRequest);

        if (userMessage == null || userMessage.isEmpty()) {
            log.debug("LC4J PII: no user message found, skipping detection");
            return;
        }

        // 构建检测上下文
        PiiContext context = new Lc4jPiiContext(null, null, Collections.emptyList(), MaskingStrategy.PARTIAL_MASK, 0.5);

        // 执行 PII 检测
        PiiDetectionResult result = piiDetector.detect(userMessage, context);

        // 将结果存入 attributes
        requestContext.attributes().put(ATTR_REQUEST_PII_RESULT, result);

        if (result.hasPii()) {
            log.info("LC4J PII: detected PII in request, types={}, riskLevel={}",
                result.getPiiTypes(), result.getRiskLevel());
        } else {
            log.debug("LC4J PII: no PII detected in request");
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ChatResponse chatResponse = responseContext.chatResponse();
        String responseContent = extractResponseContent(chatResponse);

        if (responseContent == null || responseContent.isEmpty()) {
            log.debug("LC4J PII: no response content found, skipping detection");
            return;
        }

        // 构建检测上下文
        PiiContext context = new Lc4jPiiContext(null, null, Collections.emptyList(), MaskingStrategy.PARTIAL_MASK, 0.5);

        // 执行 PII 检测
        PiiDetectionResult result = piiDetector.detect(responseContent, context);

        // 将结果存入 attributes
        responseContext.attributes().put(ATTR_RESPONSE_PII_RESULT, result);

        if (result.hasPii()) {
            log.info("LC4J PII: detected PII in response, types={}, riskLevel={}",
                result.getPiiTypes(), result.getRiskLevel());
        } else {
            log.debug("LC4J PII: no PII detected in response");
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        // PII 检测在请求和响应阶段完成，错误阶段不需要处理
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
     * 从 ChatResponse 中提取响应内容
     */
    private String extractResponseContent(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.aiMessage() == null) {
            return null;
        }
        return chatResponse.aiMessage().text();
    }

    /**
     * LangChain4J 场景下的 PiiContext 实现
     */
    private record Lc4jPiiContext(
        String userId,
        String tenantId,
        List<PiiType> detectTypes,
        MaskingStrategy maskingStrategy,
        double minConfidence
    ) implements PiiContext {

        @Override
        public String getUserId() { return userId; }

        @Override
        public String getTenantId() { return tenantId; }

        @Override
        public List<PiiType> getDetectTypes() { return detectTypes; }

        @Override
        public MaskingStrategy getMaskingStrategy() { return maskingStrategy; }

        @Override
        public double getMinConfidence() { return minConfidence; }
    }
}
