package io.github.afgprojects.framework.ai.core.pipeline;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.ChatClientRegistry;
import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.api.pipeline.StepResult;
import io.github.afgprojects.framework.ai.core.api.pipeline.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class AiChatStep implements PipelineStep {
    private final ChatClientRegistry chatClientRegistry;

    @Override
    public String getName() { return "aiChat"; }

    @Override
    public int getOrder() { return 40; }

    @Override
    public StepResult execute(PipelineContext context) {
        ApplicationConfig config = context.getConfig();
        AfgChatClient chatClient = chatClientRegistry.getDefault();

        String systemPrompt = context.getVariable("systemPrompt", String.class, "");
        String userQuestion = context.getVariable("userQuestion", String.class, context.getUserMessage());

        AfgChatClient client = chatClient;
        if (!systemPrompt.isBlank()) {
            client = client.withSystemPrompt(systemPrompt);
        }
        client = client.withModel(config.getModelId());

        AiChatResponse response = client.chat(userQuestion);
        String content = response.content();

        TokenUsage tokenUsage = new TokenUsage(0, 0, 0);
        if (response.metadata() != null) {
            var meta = response.metadata();
            long promptTokens = meta.promptTokens() != null ? meta.promptTokens() : 0;
            long completionTokens = meta.completionTokens() != null ? meta.completionTokens() : 0;
            tokenUsage = new TokenUsage(promptTokens, completionTokens, promptTokens + completionTokens);
        }

        return StepResult.ok(Map.of("assistantContent", content, "tokenUsage", tokenUsage));
    }

    public Flux<String> executeStream(PipelineContext context) {
        ApplicationConfig config = context.getConfig();
        AfgChatClient chatClient = chatClientRegistry.getDefault();

        String systemPrompt = context.getVariable("systemPrompt", String.class, "");
        String userQuestion = context.getVariable("userQuestion", String.class, context.getUserMessage());

        AfgChatClient client = chatClient;
        if (!systemPrompt.isBlank()) {
            client = client.withSystemPrompt(systemPrompt);
        }
        client = client.withModel(config.getModelId());

        return client.chatStream(userQuestion);
    }
}
