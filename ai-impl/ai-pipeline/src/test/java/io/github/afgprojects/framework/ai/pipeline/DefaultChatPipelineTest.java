package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.NoReferenceStrategy;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineContext;
import io.github.afgprojects.framework.ai.core.api.pipeline.PipelineStep;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchMode;
import io.github.afgprojects.framework.ai.core.api.pipeline.StepResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DefaultChatPipelineTest {
    @Test
    void execute_shouldRunStepsInOrder() {
        PipelineStep step1 = new PipelineStep() {
            @Override public String getName() { return "step1"; }
            @Override public int getOrder() { return 10; }
            @Override public StepResult execute(PipelineContext ctx) {
                return StepResult.ok(Map.of("key1", "value1"));
            }
        };
        PipelineStep step2 = new PipelineStep() {
            @Override public String getName() { return "step2"; }
            @Override public int getOrder() { return 20; }
            @Override public StepResult execute(PipelineContext ctx) {
                String prev = ctx.getVariable("key1", String.class, "");
                return StepResult.ok(Map.of("key2", prev + "+value2"));
            }
        };

        DefaultChatPipeline pipeline = new DefaultChatPipeline(List.of(step1, step2));
        PipelineContext context = DefaultPipelineContext.builder()
            .applicationId("app1")
            .userMessage("hello")
            .config(new TestConfig())
            .build();

        pipeline.execute(context);
        assertEquals("value1+value2", context.getVariable("key2", String.class));
    }

    @Test
    void execute_shouldSkipSkippedSteps() {
        PipelineStep skipStep = new PipelineStep() {
            @Override public String getName() { return "skipStep"; }
            @Override public int getOrder() { return 10; }
            @Override public StepResult execute(PipelineContext ctx) {
                return StepResult.skip("disabled");
            }
        };
        PipelineStep normalStep = new PipelineStep() {
            @Override public String getName() { return "normalStep"; }
            @Override public int getOrder() { return 20; }
            @Override public StepResult execute(PipelineContext ctx) {
                return StepResult.ok(Map.of("key", "value"));
            }
        };

        DefaultChatPipeline pipeline = new DefaultChatPipeline(List.of(skipStep, normalStep));
        PipelineContext context = DefaultPipelineContext.builder()
            .applicationId("app1")
            .userMessage("hello")
            .config(new TestConfig())
            .build();

        pipeline.execute(context);
        assertEquals("value", context.getVariable("key", String.class));
        assertNull(context.getVariable("skipKey", String.class));
    }

    private static class TestConfig implements ApplicationConfig {
        @Override public String getModelId() { return "default"; }
        @Override public String getSystemPrompt() { return ""; }
        @Override public String getKnowledgePrompt() { return ""; }
        @Override public String getNoKnowledgePrompt() { return ""; }
        @Override public boolean isUseKnowledgePrompt() { return false; }
        @Override public List<String> getKnowledgeIds() { return List.of(); }
        @Override public SearchMode getSearchMode() { return SearchMode.VECTOR; }
        @Override public double getSimilarityThreshold() { return 0.5; }
        @Override public int getTopN() { return 3; }
        @Override public int getMaxContentChars() { return 3000; }
        @Override public NoReferenceStrategy getNoReferenceStrategy() { return NoReferenceStrategy.AI_FOLLOW_UP; }
        @Override public boolean isEnableQuestionOptimize() { return false; }
        @Override public String getQuestionOptimizePrompt() { return null; }
        @Override public int getHistoryRounds() { return 10; }
        @Override public String getOpeningMessage() { return ""; }
        @Override public List<String> getQuickQuestions() { return List.of(); }
        @Override public boolean isEnableTts() { return false; }
        @Override public String getTtsModelId() { return null; }
        @Override public boolean isEnableStt() { return false; }
        @Override public String getSttModelId() { return null; }
        @Override public boolean isEnableTool() { return false; }
        @Override public List<String> getToolIds() { return List.of(); }
        @Override public boolean isEnableSkill() { return false; }
        @Override public List<String> getSkillIds() { return List.of(); }
        @Override public boolean isEnableMcp() { return false; }
        @Override public List<McpServerConfig> getMcpServers() { return List.of(); }
    }
}
