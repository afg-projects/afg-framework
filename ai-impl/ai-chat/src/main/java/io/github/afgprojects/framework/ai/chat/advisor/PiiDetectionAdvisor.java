package io.github.afgprojects.framework.ai.chat.advisor;

import io.github.afgprojects.framework.ai.core.security.PiiDetector;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PII 脱敏/还原 Advisor
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class PiiDetectionAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(PiiDetectionAdvisor.class);
    private static final String MASKING_TOKEN_KEY = "pii_masking_token";

    private final PiiDetector piiDetector;

    public PiiDetectionAdvisor(PiiDetector piiDetector) {
        this.piiDetector = piiDetector;
    }

    @Override
    public @NonNull String getName() {
        return "pii_detection";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public @NonNull ChatClientRequest before(@NonNull ChatClientRequest request, @NonNull AdvisorChain chain) {
        var prompt = request.prompt();
        var messages = prompt.getInstructions();
        if (messages == null || messages.isEmpty()) {
            return request;
        }

        var newMessages = new ArrayList<Message>(messages.size());
        var combinedMappings = new HashMap<String, String>();

        for (Message msg : messages) {
            if (msg instanceof UserMessage userMsg) {
                var text = userMsg.getText();
                if (text != null && !text.isBlank()) {
                    var context = createPiiContext(request.context());
                    var maskResult = piiDetector.mask(text, context);
                    if (maskResult.getMaskedCount() > 0) {
                        log.debug("PII masked: {} items, types: {}", maskResult.getMaskedCount(), maskResult.getMaskedTypes());
                        combinedMappings.putAll(maskResult.getMaskingToken().getMappings());
                        newMessages.add(UserMessage.builder()
                            .text(maskResult.getMaskedText())
                            .media(userMsg.getMedia())
                            .build());
                    } else {
                        newMessages.add(msg);
                    }
                } else {
                    newMessages.add(msg);
                }
            } else {
                newMessages.add(msg);
            }
        }

        var newContext = new HashMap<>(request.context());
        if (!combinedMappings.isEmpty()) {
            newContext.put(MASKING_TOKEN_KEY, combinedMappings);
        }

        return ChatClientRequest.builder()
            .prompt(new Prompt(newMessages, prompt.getOptions()))
            .context(newContext)
            .build();
    }

    @Override
    public @NonNull ChatClientResponse after(@NonNull ChatClientResponse response, @NonNull AdvisorChain chain) {
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

        var mappings = response.context().get(MASKING_TOKEN_KEY);
        if (mappings instanceof Map<?, ?> map && !map.isEmpty()) {
            var maskingToken = new SimpleMaskingToken(map);
            var unmaskedContent = piiDetector.unmask(content, maskingToken);
            log.debug("PII unmasked in response");

            var newAssistant = AssistantMessage.builder().content(unmaskedContent).build();
            var newGeneration = new Generation(newAssistant, result.getMetadata());
            var newChatResponse = new ChatResponse(List.of(newGeneration), chatResponse.getMetadata());

            return ChatClientResponse.builder()
                .chatResponse(newChatResponse)
                .context(response.context())
                .build();
        }

        return response;
    }

    private PiiDetector.PiiContext createPiiContext(Map<String, Object> advisorContext) {
        return new SimplePiiContext(
            (String) advisorContext.get("userId"),
            (String) advisorContext.get("tenantId")
        );
    }

    private static class SimplePiiContext implements PiiDetector.PiiContext {
        private final String userId;
        private final String tenantId;

        SimplePiiContext(String userId, String tenantId) {
            this.userId = userId;
            this.tenantId = tenantId;
        }

        @Override public String getUserId() { return userId; }
        @Override public String getTenantId() { return tenantId; }
        @Override public List<PiiDetector.PiiType> getDetectTypes() { return List.of(); }
        @Override public PiiDetector.MaskingStrategy getMaskingStrategy() { return PiiDetector.MaskingStrategy.PARTIAL_MASK; }
        @Override public double getMinConfidence() { return 0.7; }
    }

    private static class SimpleMaskingToken implements PiiDetector.MaskingToken {
        private final Map<String, String> mappings;
        private final Map<String, String> reverseMappings;
        private final String tokenId;
        private final long createdAt;

        SimpleMaskingToken(Map<?, ?> rawMappings) {
            this.mappings = new HashMap<>();
            this.reverseMappings = new HashMap<>();
            for (var entry : rawMappings.entrySet()) {
                if (entry.getKey() instanceof String k && entry.getValue() instanceof String v) {
                    mappings.put(k, v);
                    reverseMappings.put(v, k);
                }
            }
            this.tokenId = "advisor-token";
            this.createdAt = System.currentTimeMillis();
        }

        @Override public String getTokenId() { return tokenId; }
        @Override public Map<String, String> getMappings() { return mappings; }
        @Override public Map<String, String> getReverseMappings() { return reverseMappings; }
        @Override public long getCreatedAt() { return createdAt; }
        @Override public boolean isExpired() { return false; }
    }
}