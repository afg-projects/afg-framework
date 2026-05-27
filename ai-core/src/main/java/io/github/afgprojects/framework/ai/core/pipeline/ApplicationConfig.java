package io.github.afgprojects.framework.ai.core.pipeline;

import java.util.List;

public interface ApplicationConfig {
    String getModelId();
    String getSystemPrompt();
    String getKnowledgePrompt();
    String getNoKnowledgePrompt();
    boolean isUseKnowledgePrompt();
    List<String> getKnowledgeIds();
    SearchMode getSearchMode();
    double getSimilarityThreshold();
    int getTopN();
    int getMaxContentChars();
    NoReferenceStrategy getNoReferenceStrategy();
    boolean isEnableQuestionOptimize();
    String getQuestionOptimizePrompt();
    int getHistoryRounds();
    String getOpeningMessage();
    List<String> getQuickQuestions();
    boolean isEnableTts();
    String getTtsModelId();
    boolean isEnableStt();
    String getSttModelId();
    boolean isEnableTool();
    List<String> getToolIds();
    boolean isEnableSkill();
    List<String> getSkillIds();
    boolean isEnableMcp();
    List<McpServerConfig> getMcpServers();

    record McpServerConfig(String name, String url, String authToken) {}
}
