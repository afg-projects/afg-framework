package io.github.afgprojects.framework.ai.spring.chat.advisor;

import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.Ordered;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 内容安全检查 Advisor
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class ContentSafetyAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyAdvisor.class);
    private static final String INPUT_RESULT_KEY = "safety_input_result";
    private static final String OUTPUT_RESULT_KEY = "safety_output_result";
    private static final String BLOCKED_RESPONSE = "抱歉，您的内容涉及敏感信息，无法处理。";

    private final ContentSafetyChecker safetyChecker;

    public ContentSafetyAdvisor(ContentSafetyChecker safetyChecker) {
        this.safetyChecker = safetyChecker;
    }

    @Override
    public @NonNull String getName() {
        return "content_safety";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 200;
    }

    @Override
    public @NonNull ChatClientRequest before(@NonNull ChatClientRequest request, @NonNull AdvisorChain chain) {
        var userText = extractUserText(request.prompt());
        if (userText == null || userText.isBlank()) {
            return request;
        }

        var context = createSafetyContext(request.context());
        var result = safetyChecker.checkInput(userText, context);

        var newContext = new HashMap<>(request.context());
        newContext.put(INPUT_RESULT_KEY, result);

        if (result.isBlocked()) {
            log.warn("Input blocked: riskLevel={}, reason={}", result.getRiskLevel(), result.getReason());
            newContext.put("safety_blocked", true);
        }

        return ChatClientRequest.builder()
            .prompt(request.prompt())
            .context(newContext)
            .build();
    }

    @Override
    public @NonNull ChatClientResponse after(@NonNull ChatClientResponse response, @NonNull AdvisorChain chain) {
        if (Boolean.TRUE.equals(response.context().get("safety_blocked"))) {
            return createBlockedResponse(response.context());
        }

        var chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return response;
        }

        var result = chatResponse.getResult();
        if (result == null || result.getOutput() == null) {
            return response;
        }

        var content = result.getOutput().getText();
        if (content == null || content.isBlank()) {
            return response;
        }

        var context = createSafetyContext(response.context());
        var outputResult = safetyChecker.checkOutput(content, context);

        var newContext = new HashMap<>(response.context());
        newContext.put(OUTPUT_RESULT_KEY, outputResult);

        if (outputResult.isBlocked()) {
            log.warn("Output blocked: riskLevel={}, reason={}", outputResult.getRiskLevel(), outputResult.getReason());
            return createBlockedResponse(newContext);
        }

        if (outputResult.getFilteredContent() != null) {
            log.debug("Output filtered");
            var newAssistant = AssistantMessage.builder().content(outputResult.getFilteredContent()).build();
            var newGeneration = new Generation(newAssistant, result.getMetadata());
            var newChatResponse = new ChatResponse(List.of(newGeneration), chatResponse.getMetadata());

            return ChatClientResponse.builder()
                .chatResponse(newChatResponse)
                .context(newContext)
                .build();
        }

        return response;
    }

    private String extractUserText(Prompt prompt) {
        if (prompt.getInstructions() == null) {
            return null;
        }
        for (Message msg : prompt.getInstructions()) {
            if (msg instanceof UserMessage userMsg) {
                return userMsg.getText();
            }
        }
        return null;
    }

    private ContentSafetyChecker.SafetyCheckContext createSafetyContext(Map<String, Object> advisorContext) {
        return new SimpleSafetyContext(
            (String) advisorContext.get("userId"),
            (String) advisorContext.get("tenantId"),
            (String) advisorContext.get("modelName")
        );
    }

    private ChatClientResponse createBlockedResponse(Map<String, Object> context) {
        var assistant = AssistantMessage.builder().content(BLOCKED_RESPONSE).build();
        var generation = new Generation(assistant);
        var chatResponse = new ChatResponse(List.of(generation));
        return ChatClientResponse.builder()
            .chatResponse(chatResponse)
            .context(context)
            .build();
    }

    private static class SimpleSafetyContext implements ContentSafetyChecker.SafetyCheckContext {
        private final String userId;
        private final String tenantId;
        private final String modelName;

        SimpleSafetyContext(String userId, String tenantId, String modelName) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.modelName = modelName;
        }

        @Override public String getUserId() { return userId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public String getModelName() { return modelName; }
        @Override public String getOperationType() { return "chat"; }
        @Override public boolean isStrictMode() { return false; }
        @Override public List<String> getCheckCategories() { return List.of(); }
    }
}
