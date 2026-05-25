package io.github.afgprojects.framework.ai.agent.skill.intent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.chat.AiMessage;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 默认意图分析器实现
 *
 * <p>使用 LLM 分析用户意图，匹配最合适的 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultIntentAnalyzer implements IntentAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(DefaultIntentAnalyzer.class);

    private static final double CLARIFICATION_THRESHOLD = 0.15; // 置信度差距小于此值需要澄清
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.5; // 最低置信度阈值

    private final AfgChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final double clarificationThreshold;
    private final double minConfidenceThreshold;

    public DefaultIntentAnalyzer(@NonNull AfgChatClient chatClient) {
        this(chatClient, new ObjectMapper(), CLARIFICATION_THRESHOLD, MIN_CONFIDENCE_THRESHOLD);
    }

    public DefaultIntentAnalyzer(
        @NonNull AfgChatClient chatClient,
        @NonNull ObjectMapper objectMapper,
        double clarificationThreshold,
        double minConfidenceThreshold
    ) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.clarificationThreshold = clarificationThreshold;
        this.minConfidenceThreshold = minConfidenceThreshold;
    }

    private static final String INTENT_ANALYSIS_SYSTEM_PROMPT = """
        你是一个意图分析助手，负责分析用户输入并匹配最合适的 Skill。

        你需要：
        1. 理解用户的意图
        2. 从可用的 Skills 中选择最匹配的一个或多个
        3. 为每个匹配的 Skill 给出 0-1 的置信度评分
        4. 从用户输入中提取可能的参数

        请以 JSON 格式返回结果，格式如下：
        ```json
        {
          "intentDescription": "用户意图的简要描述",
          "matches": [
            {
              "skillName": "Skill名称",
              "confidence": 0.95,
              "matchReason": "匹配原因",
              "parameters": {"参数名": "参数值"}
            }
          ],
          "needsClarification": false,
          "clarificationQuestion": null
        }
        ```

        规则：
        - 如果有多个 Skills 置信度相近（差距小于 0.15），设置 needsClarification 为 true 并提供澄清问题
        - 置信度低于 0.5 的匹配不应推荐
        - 参数提取要准确，不要编造不存在的参数
        """;

    @Override
    @NonNull
    public IntentResult analyze(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> availableSkills,
        @NonNull SkillContext context
    ) {
        log.debug("Analyzing intent for user input: {}", userInput);

        if (availableSkills.isEmpty()) {
            return IntentResult.noMatch("没有可用的 Skills");
        }

        // 构建 Skills 描述
        String skillsDescription = buildSkillsDescription(availableSkills);

        // 构建分析请求
        String analysisPrompt = """
            可用的 Skills：
            %s

            用户输入：%s

            请分析用户意图并返回 JSON 格式的结果。
            """.formatted(skillsDescription, userInput);

        // 调用 LLM 进行分析
        AiChatResponse response = chatClient
            .withSystemPrompt(INTENT_ANALYSIS_SYSTEM_PROMPT)
            .prompt(analysisPrompt)
            .options(Map.of("temperature", 0.3))
            .call();

        String responseContent = response.content();

        log.debug("LLM response: {}", responseContent);

        // 解析分析结果
        return parseIntentResult(responseContent, availableSkills);
    }

    @Override
    @NonNull
    public List<SkillMatch> matchSkills(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> availableSkills
    ) {
        if (availableSkills.isEmpty()) {
            return List.of();
        }

        // 简单的关键词匹配作为快速路径
        List<SkillMatch> keywordMatches = keywordMatch(userInput, availableSkills);

        // 如果关键词匹配置信度很高，直接返回
        if (!keywordMatches.isEmpty() && keywordMatches.get(0).confidence() > 0.8) {
            return keywordMatches;
        }

        // 否则使用 LLM 进行语义匹配
        return semanticMatch(userInput, availableSkills);
    }

    /**
     * 关键词匹配
     */
    private List<SkillMatch> keywordMatch(String userInput, List<SkillDefinition> skills) {
        List<SkillMatch> matches = new ArrayList<>();
        String lowerInput = userInput.toLowerCase();

        for (SkillDefinition skill : skills) {
            double confidence = calculateKeywordConfidence(lowerInput, skill);
            if (confidence > 0.3) {
                matches.add(SkillMatch.of(skill, confidence, "关键词匹配"));
            }
        }

        return matches.stream()
            .sorted(Comparator.comparingDouble(SkillMatch::confidence).reversed())
            .collect(Collectors.toList());
    }

    /**
     * 计算关键词匹配置信度
     */
    private double calculateKeywordConfidence(String lowerInput, SkillDefinition skill) {
        String skillName = skill.name().toLowerCase();
        String description = skill.description().toLowerCase();

        double confidence = 0.0;

        // Skill 名称完全匹配
        if (lowerInput.contains(skillName)) {
            confidence += 0.5;
        }

        // 描述中的关键词匹配
        String[] keywords = description.split("[\\s,，。.！!？?]+");
        for (String keyword : keywords) {
            if (keyword.length() > 1 && lowerInput.contains(keyword)) {
                confidence += 0.1;
            }
        }

        return Math.min(confidence, 1.0);
    }

    /**
     * 语义匹配（使用 LLM）
     */
    private List<SkillMatch> semanticMatch(String userInput, List<SkillDefinition> skills) {
        // 简化实现：返回关键词匹配结果
        // 完整实现应该调用 LLM 进行语义分析
        return keywordMatch(userInput, skills);
    }

    /**
     * 构建 Skills 描述
     */
    private String buildSkillsDescription(List<SkillDefinition> skills) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < skills.size(); i++) {
            SkillDefinition skill = skills.get(i);
            sb.append(i + 1).append(". **").append(skill.name()).append("**\n");
            sb.append("   描述：").append(skill.description()).append("\n");
            if (skill.inputs() != null && !skill.inputs().isEmpty()) {
                sb.append("   参数：");
                for (var input : skill.inputs()) {
                    sb.append(input.name());
                    if (input.required()) {
                        sb.append("(必填)");
                    }
                    sb.append(", ");
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析意图分析结果
     */
    private IntentResult parseIntentResult(String responseContent, List<SkillDefinition> availableSkills) {
        try {
            // 提取 JSON 内容
            String jsonContent = extractJson(responseContent);
            if (jsonContent == null) {
                return IntentResult.noMatch("无法解析 LLM 响应");
            }

            Map<String, Object> result = objectMapper.readValue(jsonContent, new TypeReference<>() {});

            String intentDescription = (String) result.getOrDefault("intentDescription", "");
            boolean needsClarification = Boolean.TRUE.equals(result.get("needsClarification"));
            String clarificationQuestion = (String) result.get("clarificationQuestion");

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> matches = (List<Map<String, Object>>) result.get("matches");

            List<SkillMatch> skillMatches = new ArrayList<>();
            if (matches != null) {
                for (Map<String, Object> match : matches) {
                    String skillName = (String) match.get("skillName");
                    double confidence = ((Number) match.getOrDefault("confidence", 0.0)).doubleValue();
                    String matchReason = (String) match.get("matchReason");

                    @SuppressWarnings("unchecked")
                    Map<String, Object> parameters = (Map<String, Object>) match.getOrDefault("parameters", Map.of());

                    // 查找对应的 Skill
                    availableSkills.stream()
                        .filter(s -> s.name().equals(skillName))
                        .findFirst()
                        .ifPresent(skill -> skillMatches.add(SkillMatch.of(skill, confidence, matchReason, parameters)));
                }
            }

            // 按置信度排序
            skillMatches.sort(Comparator.comparingDouble(SkillMatch::confidence).reversed());

            // 检查是否需要澄清
            if (skillMatches.size() >= 2) {
                double topConfidence = skillMatches.get(0).confidence();
                double secondConfidence = skillMatches.get(1).confidence();
                if (topConfidence - secondConfidence < clarificationThreshold) {
                    needsClarification = true;
                    if (clarificationQuestion == null) {
                        clarificationQuestion = "您是想「" + skillMatches.get(0).skill().description() +
                            "」还是「" + skillMatches.get(1).skill().description() + "」？";
                    }
                }
            }

            // 检查最低置信度
            if (!skillMatches.isEmpty() && skillMatches.get(0).confidence() < minConfidenceThreshold) {
                return IntentResult.noMatch(intentDescription + "（置信度过低）");
            }

            // 构建结果
            if (needsClarification) {
                return IntentResult.needsClarification(intentDescription, skillMatches, clarificationQuestion);
            }

            if (skillMatches.isEmpty()) {
                return IntentResult.noMatch(intentDescription);
            }

            SkillMatch recommended = skillMatches.get(0);
            return IntentResult.matched(intentDescription, recommended, skillMatches, recommended.suggestedParameters());

        } catch (Exception e) {
            log.error("Failed to parse intent result: {}", e.getMessage());
            return IntentResult.noMatch("解析意图失败: " + e.getMessage());
        }
    }

    /**
     * 从响应中提取 JSON 内容
     */
    private String extractJson(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        // 尝试提取 ```json ... ``` 块
        int jsonStart = content.indexOf("```json");
        if (jsonStart >= 0) {
            jsonStart += 7;
            int jsonEnd = content.indexOf("```", jsonStart);
            if (jsonEnd > jsonStart) {
                return content.substring(jsonStart, jsonEnd).trim();
            }
        }

        // 尝试提取 ``` ... ``` 块
        jsonStart = content.indexOf("```");
        if (jsonStart >= 0) {
            jsonStart += 3;
            int jsonEnd = content.indexOf("```", jsonStart);
            if (jsonEnd > jsonStart) {
                return content.substring(jsonStart, jsonEnd).trim();
            }
        }

        // 尝试直接解析整个内容
        int braceStart = content.indexOf('{');
        int braceEnd = content.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return content.substring(braceStart, braceEnd + 1);
        }

        return null;
    }
}
