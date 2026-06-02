package io.github.afgprojects.framework.ai.core.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.ChatPipeline;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineResult;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchResult;
import io.github.afgprojects.framework.ai.core.api.pipeline.SourceReference;
import io.github.afgprojects.framework.ai.core.api.pipeline.StepResult;
import io.github.afgprojects.framework.ai.core.api.pipeline.TokenUsage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DefaultChatPipeline implements ChatPipeline {
    private final List<PipelineStep> steps;

    @Override
    public PipelineResult execute(PipelineContext context) {
        long start = System.currentTimeMillis();
        for (PipelineStep step : steps) {
            StepResult result = step.execute(context);
            if (result.skipped()) {
                log.debug("Step {} skipped: {}", step.getName(), result.skipReason());
                continue;
            }
            if (result.outputVariables() != null) {
                result.outputVariables().forEach(context::setVariable);
            }
        }
        return buildResult(context, System.currentTimeMillis() - start);
    }

    @Override
    public Flux<String> executeStream(PipelineContext context) {
        List<PipelineStep> preChatSteps = steps.stream()
            .filter(s -> s.getOrder() < 40)
            .sorted(Comparator.comparingInt(PipelineStep::getOrder))
            .toList();
        for (PipelineStep step : preChatSteps) {
            StepResult result = step.execute(context);
            if (!result.skipped() && result.outputVariables() != null) {
                result.outputVariables().forEach(context::setVariable);
            }
        }

        AiChatStep chatStep = steps.stream()
            .filter(s -> s instanceof AiChatStep)
            .map(s -> (AiChatStep) s)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No AiChatStep found in pipeline"));

        return chatStep.executeStream(context);
    }

    private PipelineResult buildResult(PipelineContext context, long durationMs) {
        String content = context.getVariable("assistantContent", String.class, "");
        TokenUsage tokenUsage = context.getVariable("tokenUsage", TokenUsage.class, new TokenUsage(0, 0, 0));
        String optimizedQuestion = context.getOptimizedQuestion();
        List<SearchResult> searchResults = context.getSearchResults();
        List<SourceReference> sources = searchResults.stream()
            .map(r -> new SourceReference(r.chunkId(), r.documentId(), r.documentTitle(), r.content(), r.score(), r.knowledgeBaseName()))
            .toList();

        return new PipelineResult(content, context.getConversationId(), sources, tokenUsage, optimizedQuestion, durationMs, Map.of());
    }
}
