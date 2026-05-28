package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.pipeline.NoReferenceStrategy;
import io.github.afgprojects.framework.ai.core.pipeline.SearchMode;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class SimpleApplicationConfig implements ApplicationConfig {
    private final SimpleConfig simpleConfig;

    @Override public String getModelId() { return simpleConfig.getModelId(); }
    @Override public String getSystemPrompt() { return simpleConfig.getSystemPrompt(); }
    @Override public String getKnowledgePrompt() { return simpleConfig.getKnowledgePrompt(); }
    @Override public String getNoKnowledgePrompt() { return simpleConfig.getNoKnowledgePrompt(); }
    @Override public boolean isUseKnowledgePrompt() { return simpleConfig.isUseKnowledgePrompt(); }
    @Override public List<String> getKnowledgeIds() { return simpleConfig.getKnowledgeIds(); }
    @Override public SearchMode getSearchMode() { return simpleConfig.getSearchMode(); }
    @Override public double getSimilarityThreshold() { return simpleConfig.getSimilarityThreshold(); }
    @Override public int getTopN() { return simpleConfig.getTopN(); }
    @Override public int getMaxContentChars() { return simpleConfig.getMaxContentChars(); }
    @Override public NoReferenceStrategy getNoReferenceStrategy() { return simpleConfig.getNoReferenceStrategy(); }
    @Override public boolean isEnableQuestionOptimize() { return simpleConfig.isEnableQuestionOptimize(); }
    @Override public String getQuestionOptimizePrompt() { return simpleConfig.getQuestionOptimizePrompt(); }
    @Override public int getHistoryRounds() { return simpleConfig.getHistoryRounds(); }
    @Override public String getOpeningMessage() { return simpleConfig.getOpeningMessage(); }
    @Override public List<String> getQuickQuestions() { return simpleConfig.getQuickQuestions(); }
    @Override public boolean isEnableTts() { return simpleConfig.isEnableTts(); }
    @Override public String getTtsModelId() { return simpleConfig.getTtsModelId(); }
    @Override public boolean isEnableStt() { return simpleConfig.isEnableStt(); }
    @Override public String getSttModelId() { return simpleConfig.getSttModelId(); }
    @Override public boolean isEnableTool() { return simpleConfig.isEnableTool(); }
    @Override public List<String> getToolIds() { return simpleConfig.getToolIds(); }
    @Override public boolean isEnableSkill() { return simpleConfig.isEnableSkill(); }
    @Override public List<String> getSkillIds() { return simpleConfig.getSkillIds(); }
    @Override public boolean isEnableMcp() { return simpleConfig.isEnableMcp(); }
    @Override public List<McpServerConfig> getMcpServers() { return simpleConfig.getMcpServers(); }
}
