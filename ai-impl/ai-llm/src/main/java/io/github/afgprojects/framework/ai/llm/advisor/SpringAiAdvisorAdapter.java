package io.github.afgprojects.framework.ai.llm.advisor;

import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 将 AFG LlmAdvisor 适配为 Spring AI BaseAdvisor
 *
 * <p>使得 AFG 的 Advisor 可以在 Spring AI ChatClient 的 Advisor 链中执行。
 * 在 before 阶段调用 LlmAdvisor.apply() 修改消息列表，
 * 在 after 阶段调用 LlmAdvisor.onResponse() 处理响应。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class SpringAiAdvisorAdapter implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SpringAiAdvisorAdapter.class);

    private final LlmAdvisor delegate;

    public SpringAiAdvisorAdapter(@NonNull LlmAdvisor delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getName() {
        return "AFG-" + delegate.getName();
    }

    @Override
    public int getOrder() {
        return delegate.getOrder();
    }

    @Override
    @NonNull
    public ChatClientRequest before(@NonNull ChatClientRequest request, @NonNull AdvisorChain chain) {
        log.debug("SpringAiAdvisorAdapter.before: {}", delegate.getName());

        List<Message> currentMessages = request.prompt().getInstructions();
        Map<String, Object> context = new HashMap<>(request.context());
        LlmAdvisor.AdvisorContext advisorContext = new MapAdvisorContext(context);

        List<Message> modifiedMessages = delegate.apply(currentMessages, advisorContext);

        Prompt modifiedPrompt = new Prompt(modifiedMessages, request.prompt().getOptions());

        return ChatClientRequest.builder()
                .prompt(modifiedPrompt)
                .context(context)
                .build();
    }

    @Override
    @NonNull
    public ChatClientResponse after(@NonNull ChatClientResponse response, @NonNull AdvisorChain chain) {
        log.debug("SpringAiAdvisorAdapter.after: {}", delegate.getName());

        try {
            String content = "";
            if (response.chatResponse() != null
                    && response.chatResponse().getResult() != null
                    && response.chatResponse().getResult().getOutput() != null) {
                content = response.chatResponse().getResult().getOutput().getText();
            }

            LlmResponse llmResponse = new LlmResponse(content, List.of(), List.of(), null, null);
            LlmAdvisor.AdvisorContext advisorContext = new MapAdvisorContext(Map.of());

            delegate.onResponse(llmResponse, advisorContext);
        } catch (Exception e) {
            log.warn("Advisor {} onResponse failed: {}", delegate.getName(), e.getMessage());
        }

        return response;
    }

    /**
     * 获取被适配的 AFG LlmAdvisor
     */
    @NonNull
    public LlmAdvisor getDelegate() {
        return delegate;
    }

    private static class MapAdvisorContext implements LlmAdvisor.AdvisorContext {
        private final Map<String, Object> attributes;
        private final io.github.afgprojects.framework.ai.core.model.LlmRequest request;

        MapAdvisorContext(Map<String, Object> attributes) {
            this.attributes = attributes;
            this.request = io.github.afgprojects.framework.ai.core.model.LlmRequest.ofUserMessage("");
        }

        @Override
        public io.github.afgprojects.framework.ai.core.model.LlmRequest getRequest() {
            return request;
        }

        @Override
        public String getSessionId() {
            return (String) attributes.get("sessionId");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getAttribute(String key) {
            return (T) attributes.get(key);
        }

        @Override
        public void setAttribute(String key, Object value) {
            attributes.put(key, value);
        }
    }
}
