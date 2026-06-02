package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.skill.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

/**
 * 默认意图分析器实现
 *
 * <p>使用 LLM 分析用户意图，匹配对应的 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultIntentAnalyzer implements IntentAnalyzer {

    private static final String ANALYSIS_PROMPT = """
            Analyze the following user input and determine which skill(s) it matches.

            User input: %s

            Available skills:
            %s

            Respond in the following JSON format:
            ```json
            {
              "matches": [
                {
                  "skill": "skill_name",
                  "confidence": 0.95,
                  "reason": "why this skill matches",
                  "parameters": {"key": "value"}
                }
              ]
            }
            ```

            Only include matches with confidence > 0.3.
            Sort by confidence descending.
            """;

    private final SkillRegistry skillRegistry;
    private final AfgChatClient chatClient;

    public DefaultIntentAnalyzer(
            @NonNull SkillRegistry skillRegistry,
            @NonNull AfgChatClient chatClient
    ) {
        this.skillRegistry = skillRegistry;
        this.chatClient = chatClient;
    }

    @Override
    @NonNull
    public IntentResult analyze(@NonNull String input, @NonNull SkillContext context) {
        return analyze(input);
    }

    @Override
    @NonNull
    public IntentResult analyze(@NonNull String input) {
        log.debug("Analyzing intent for input: {}", input);

        List<SkillDefinition> availableSkills = skillRegistry.getAll();
        if (availableSkills.isEmpty()) {
            return IntentResult.empty();
        }

        // 构建分析提示
        String skillDescriptions = buildSkillDescriptions(availableSkills);
        String prompt = String.format(ANALYSIS_PROMPT, input, skillDescriptions);

        try {
            // 调用 LLM
            AiChatResponse response = chatClient
                    .withSystemPrompt("You are a skill matching assistant. Analyze user input and match to available skills.")
                    .chat(prompt);

            // 解析结果
            return parseAnalysisResult(response.content(), availableSkills);

        } catch (Exception e) {
            log.error("Intent analysis failed: {}", e.getMessage());
            return IntentResult.empty();
        }
    }

    private @NonNull String buildSkillDescriptions(@NonNull List<SkillDefinition> skills) {
        StringBuilder sb = new StringBuilder();
        for (SkillDefinition skill : skills) {
            sb.append("- ").append(skill.name()).append(": ").append(skill.description());
            if (skill.inputs() != null && !skill.inputs().isEmpty()) {
                sb.append(" (inputs: ");
                sb.append(String.join(", ",
                        skill.inputs().stream().map(SkillDefinition.InputParameter::name).toList()));
                sb.append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private @NonNull IntentResult parseAnalysisResult(@NonNull String content, @NonNull List<SkillDefinition> availableSkills) {
        List<SkillMatch> matches = new ArrayList<>();
        Map<String, SkillDefinition> skillMap = new HashMap<>();
        for (SkillDefinition skill : availableSkills) {
            skillMap.put(skill.name(), skill);
        }

        // 简单的 JSON 解析（避免引入复杂依赖）
        try {
            String json = extractJson(content);
            if (json == null) {
                return IntentResult.empty();
            }

            // 解析 matches 数组
            var matchSections = extractMatchSections(json);
            for (String section : matchSections) {
                String skillName = extractValue(section, "skill");
                if (skillName == null) continue;

                SkillDefinition definition = skillMap.get(skillName);
                if (definition == null) continue;

                double confidence = extractDoubleValue(section, "confidence", 0.5);
                String reason = extractValue(section, "reason");

                // 提取参数
                Map<String, Object> parameters = extractParameters(section);

                matches.add(SkillMatch.of(definition, confidence, reason, parameters));
            }

        } catch (Exception e) {
            log.warn("Failed to parse intent analysis result: {}", e.getMessage());
        }

        // 按置信度降序排序
        matches.sort(Comparator.comparingDouble(SkillMatch::confidence).reversed());

        return IntentResult.of(matches, content);
    }

    private @Nullable String extractJson(@NonNull String content) {
        int start = content.indexOf("```json");
        if (start >= 0) {
            start = content.indexOf('\n', start) + 1;
            int end = content.indexOf("```", start);
            if (end > start) {
                return content.substring(start, end).trim();
            }
        }

        start = content.indexOf('{');
        if (start >= 0) {
            int end = content.lastIndexOf('}');
            if (end > start) {
                return content.substring(start, end + 1);
            }
        }

        return null;
    }

    private @NonNull List<String> extractMatchSections(@NonNull String json) {
        List<String> sections = new ArrayList<>();
        int matchesStart = json.indexOf("\"matches\"");
        if (matchesStart < 0) return sections;

        int arrayStart = json.indexOf('[', matchesStart);
        int arrayEnd = json.indexOf(']', arrayStart);
        if (arrayStart < 0 || arrayEnd < 0) return sections;

        String arrayContent = json.substring(arrayStart + 1, arrayEnd);

        int depth = 0;
        int sectionStart = -1;
        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                if (depth == 0) sectionStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && sectionStart >= 0) {
                    sections.add(arrayContent.substring(sectionStart, i + 1));
                    sectionStart = -1;
                }
            }
        }

        return sections;
    }

    private @Nullable String extractValue(@NonNull String section, @NonNull String key) {
        String pattern = "\"" + key + "\"";
        int keyIdx = section.indexOf(pattern);
        if (keyIdx < 0) return null;

        int colonIdx = section.indexOf(':', keyIdx + pattern.length());
        if (colonIdx < 0) return null;

        int valueStart = section.indexOf('"', colonIdx + 1);
        if (valueStart < 0) return null;

        int valueEnd = section.indexOf('"', valueStart + 1);
        if (valueEnd < 0) return null;

        return section.substring(valueStart + 1, valueEnd);
    }

    private double extractDoubleValue(@NonNull String section, @NonNull String key, double defaultValue) {
        String pattern = "\"" + key + "\"";
        int keyIdx = section.indexOf(pattern);
        if (keyIdx < 0) return defaultValue;

        int colonIdx = section.indexOf(':', keyIdx + pattern.length());
        if (colonIdx < 0) return defaultValue;

        StringBuilder numStr = new StringBuilder();
        for (int i = colonIdx + 1; i < section.length(); i++) {
            char c = section.charAt(i);
            if (c == '.' || Character.isDigit(c)) {
                numStr.append(c);
            } else if (numStr.length() > 0) {
                break;
            }
        }

        if (numStr.isEmpty()) return defaultValue;

        try {
            return Double.parseDouble(numStr.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private @NonNull Map<String, Object> extractParameters(@NonNull String section) {
        String pattern = "\"parameters\"";
        int keyIdx = section.indexOf(pattern);
        if (keyIdx < 0) return Map.of();

        int colonIdx = section.indexOf(':', keyIdx + pattern.length());
        if (colonIdx < 0) return Map.of();

        int objStart = section.indexOf('{', colonIdx);
        int objEnd = section.indexOf('}', objStart);
        if (objStart < 0 || objEnd < 0) return Map.of();

        String paramSection = section.substring(objStart + 1, objEnd);
        Map<String, Object> params = new HashMap<>();

        // 简单键值对解析
        String[] pairs = paramSection.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String k = kv[0].replace("\"", "").trim();
                String v = kv[1].replace("\"", "").trim();
                params.put(k, v);
            }
        }

        return params;
    }
}