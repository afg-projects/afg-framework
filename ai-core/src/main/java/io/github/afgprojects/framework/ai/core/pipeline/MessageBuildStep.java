package io.github.afgprojects.framework.ai.core.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.api.pipeline.StepResult;

import java.util.Map;

public class MessageBuildStep implements PipelineStep {
    @Override
    public String getName() { return "messageBuild"; }

    @Override
    public int getOrder() { return 30; }

    @Override
    public StepResult execute(PipelineContext context) {
        ApplicationConfig config = context.getConfig();
        Boolean hasReference = context.getVariable("hasReference", Boolean.class, false);
        String referenceContent = context.getReferenceContent();
        String userQuestion = context.getOptimizedQuestion();
        String systemPrompt = buildSystemPrompt(config, hasReference, referenceContent);
        return StepResult.ok(Map.of("systemPrompt", systemPrompt, "userQuestion", userQuestion));
    }

    private String buildSystemPrompt(ApplicationConfig config, boolean hasReference, String referenceContent) {
        StringBuilder prompt = new StringBuilder();
        if (config.getSystemPrompt() != null && !config.getSystemPrompt().isBlank()) {
            prompt.append(config.getSystemPrompt());
        }
        if (hasReference && config.isUseKnowledgePrompt()) {
            if (prompt.length() > 0) prompt.append("\n\n");
            prompt.append(config.getKnowledgePrompt().replace("{data}", referenceContent));
        } else if (!hasReference && config.isUseKnowledgePrompt()) {
            if (prompt.length() > 0) prompt.append("\n\n");
            prompt.append(config.getNoKnowledgePrompt());
        }
        return prompt.toString();
    }
}
