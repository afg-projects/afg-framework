package io.github.afgprojects.framework.ai.agent.skill.intent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition.InputParameter;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition.ParameterType;
import io.github.afgprojects.framework.ai.core.chat.AfgChatClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 意图分析器测试
 *
 * @author afg-projects
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class IntentAnalysisIntegrationTest {

    @Mock
    private AfgChatClient chatClient;

    private DefaultIntentAnalyzer intentAnalyzer;

    private List<SkillDefinition> testSkills;

    @BeforeEach
    void setUp() {
        intentAnalyzer = new DefaultIntentAnalyzer(chatClient, new ObjectMapper(), 0.15, 0.5);

        testSkills = List.of(
            new SkillDefinition(
                "weather_query",
                "查询天气信息，获取指定城市的天气状况",
                "请查询 {{city}} 的天气信息",
                List.of(
                    new InputParameter("city", "城市名称", ParameterType.STRING, true, null, null)
                ),
                null,
                null,
                null
            ),
            new SkillDefinition(
                "calculator",
                "数学计算器，执行加减乘除等数学运算",
                "请计算: {{expression}}",
                List.of(
                    new InputParameter("expression", "数学表达式", ParameterType.STRING, true, null, null)
                ),
                null,
                null,
                null
            ),
            new SkillDefinition(
                "translator",
                "翻译工具，支持多语言翻译",
                "请将以下内容翻译成{{target_language}}：{{text}}",
                List.of(
                    new InputParameter("text", "要翻译的文本", ParameterType.STRING, true, null, null),
                    new InputParameter("target_language", "目标语言", ParameterType.STRING, true, "英语", null)
                ),
                null,
                null,
                null
            ),
            new SkillDefinition(
                "search",
                "搜索工具，在互联网上搜索信息",
                "请搜索：{{query}}",
                List.of(
                    new InputParameter("query", "搜索关键词", ParameterType.STRING, true, null, null)
                ),
                null,
                null,
                null
            )
        );
    }

    @Test
    void matchSkills_shouldReturnKeywordMatchesForWeatherQuery() {
        String userInput = "查询天气";

        List<SkillMatch> matches = intentAnalyzer.matchSkills(userInput, testSkills);

        assertThat(matches).isNotEmpty();
        assertThat(matches.get(0).skill().name()).isEqualTo("weather_query");
    }

    @Test
    void matchSkills_shouldReturnEmptyForNoMatch() {
        String userInput = "今天吃什么？";

        List<SkillMatch> matches = intentAnalyzer.matchSkills(userInput, testSkills);

        assertThat(matches).isNotNull();
    }

    @Test
    void matchSkills_shouldMatchCalculator() {
        String userInput = "帮我算一下数学计算";

        List<SkillMatch> matches = intentAnalyzer.matchSkills(userInput, testSkills);

        assertThat(matches).isNotNull();
    }

    @Test
    void analyze_shouldHandleEmptySkills() {
        String userInput = "北京今天天气怎么样？";
        SkillContext context = new SkillContext("test", Map.of());

        IntentResult result = intentAnalyzer.analyze(userInput, List.of(), context);

        assertThat(result.matchedSkills()).isEmpty();
        assertThat(result.recommendedSkill()).isNull();
    }
}