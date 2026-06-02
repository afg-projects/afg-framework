package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.api.pipeline.StepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class QuestionOptimizeStep implements PipelineStep {
    private final AfgChatClient chatClient;

    private static final String DEFAULT_OPTIMIZE_PROMPT = """
        根据对话历史，将用户的问题优化为一个更完整、更清晰的问题。
        直接输出优化后的问题，不要解释。

        对话历史：
        {history}

        用户问题：{question}
        """;

    @Override
    public String getName() { return "questionOptimize"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public StepResult execute(PipelineContext context) {
        if (!context.getConfig().isEnableQuestionOptimize()) {
            return StepResult.skip("问题优化未启用");
        }
        try {
            String prompt = context.getConfig().getQuestionOptimizePrompt();
            if (prompt == null || prompt.isBlank()) {
                prompt = DEFAULT_OPTIMIZE_PROMPT;
            }
            String history = context.getVariable("historyContext", String.class, "");
            String systemPrompt = prompt.replace("{history}", history).replace("{question}", context.getUserMessage());
            String optimized = chatClient.withSystemPrompt(systemPrompt).chat(context.getUserMessage()).content();
            return StepResult.ok(Map.of("optimizedQuestion", optimized));
        } catch (Exception e) {
            log.warn("问题优化失败，使用原始问题: {}", e.getMessage());
            return StepResult.ok(Map.of("optimizedQuestion", context.getUserMessage()));
        }
    }
}
