package io.github.afgprojects.framework.ai.pipeline;

import io.github.afgprojects.framework.ai.core.api.pipeline.ApplicationConfig;
import io.github.afgprojects.framework.ai.core.api.pipeline.NoReferenceStrategy;
import io.github.afgprojects.framework.ai.core.api.pipeline.SearchMode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SimpleConfig {
    private String modelId = "";
    private String systemPrompt = "";
    private String knowledgePrompt = "参考以下信息回答用户问题：\n{data}";
    private String noKnowledgePrompt = "抱歉，我无法回答与知识库无关的问题。";
    private boolean useKnowledgePrompt = true;
    private NoReferenceStrategy noReferenceStrategy = NoReferenceStrategy.AI_FOLLOW_UP;
    private List<String> knowledgeIds = List.of();
    private SearchMode searchMode = SearchMode.VECTOR;
    private double similarityThreshold = 0.5;
    private int topN = 3;
    private int maxContentChars = 3000;
    private boolean enableQuestionOptimize = false;
    private String questionOptimizePrompt;
    private int historyRounds = 10;
    private String openingMessage = "";
    private List<String> quickQuestions = List.of();
    private boolean enableTts = false;
    private String ttsModelId;
    private boolean enableStt = false;
    private String sttModelId;
    private boolean enableTool = false;
    private List<String> toolIds = List.of();
    private boolean enableSkill = false;
    private List<String> skillIds = List.of();
    private boolean enableMcp = false;
    private List<ApplicationConfig.McpServerConfig> mcpServers = List.of();
}
