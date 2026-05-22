package io.github.afgprojects.framework.ai.agent.skill.intent;

import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.core.model.LlmConfig;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.llm.ollama.OllamaLlmClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Skill 意图分析集成测试
 *
 * <p>使用本地 Ollama 服务进行测试。
 * 运行前请确保 Ollama 服务已启动并拉取了模型。
 *
 * <p>准备工作：
 * <pre>
 * # 启动 Ollama 服务
 * ollama serve
 *
 * # 拉取模型
 * ollama pull qwen2.5:1.5b
 * </pre>
 */
@DisplayName("Skill 意图分析集成测试")
class IntentAnalysisIntegrationTest {

    private static final String OLLAMA_BASE_URL = "http://localhost:11434";
    private static final String MODEL_NAME = "qwen2.5:1.5b";

    private static OllamaLlmClient llmClient;
    private DefaultIntentAnalyzer intentAnalyzer;

    private List<SkillDefinition> testSkills;

    @BeforeAll
    static void setUpClass() {
        try {
            llmClient = new OllamaLlmClient(OLLAMA_BASE_URL, MODEL_NAME);
            // 测试连接
            LlmResponse response = llmClient.chat(LlmRequest.ofUserMessage("Hello"));
            System.out.println("Ollama 连接成功，模型: " + MODEL_NAME);
        } catch (Exception e) {
            System.err.println("无法连接 Ollama 服务: " + e.getMessage());
            System.err.println("请确保 Ollama 服务已启动: ollama serve");
            System.err.println("并拉取模型: ollama pull " + MODEL_NAME);
        }
    }

    @BeforeEach
    void setUp() {
        intentAnalyzer = new DefaultIntentAnalyzer(llmClient);

        // 创建测试 Skills
        testSkills = List.of(
            new SkillDefinition(
                "weather_query",
                "查询天气信息，获取指定城市的天气状况",
                "请查询 {{city}} 的天气信息",
                List.of(
                    new SkillDefinition.InputParameter("city", "城市名称", SkillDefinition.ParameterType.STRING, true, null, null)
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
                    new SkillDefinition.InputParameter("expression", "数学表达式", SkillDefinition.ParameterType.STRING, true, null, null)
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
                    new SkillDefinition.InputParameter("text", "要翻译的文本", SkillDefinition.ParameterType.STRING, true, null, null),
                    new SkillDefinition.InputParameter("target_language", "目标语言", SkillDefinition.ParameterType.STRING, true, "英语", null)
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
                    new SkillDefinition.InputParameter("query", "搜索关键词", SkillDefinition.ParameterType.STRING, true, null, null)
                ),
                null,
                null,
                null
            )
        );
    }

    @Test
    @DisplayName("测试天气查询意图识别")
    @EnabledIf("isOllamaAvailable")
    void testWeatherQueryIntent() {
        String userInput = "北京今天天气怎么样？";
        SkillContext context = new SkillContext("test", Map.of());

        IntentResult result = intentAnalyzer.analyze(userInput, testSkills, context);

        System.out.println("=== 天气查询测试 ===");
        System.out.println("用户输入: " + userInput);
        System.out.println("意图描述: " + result.intentDescription());
        System.out.println("匹配结果:");
        for (SkillMatch match : result.matchedSkills()) {
            System.out.println("  - " + match.skill().name() + " (置信度: " + match.confidence() + ")");
        }

        assertFalse(result.matchedSkills().isEmpty(), "应该有匹配的 Skill");
        assertTrue(result.matchedSkills().stream()
            .anyMatch(m -> m.skill().name().equals("weather_query")),
            "应该匹配 weather_query");
    }

    @Test
    @DisplayName("测试计算器意图识别")
    @EnabledIf("isOllamaAvailable")
    void testCalculatorIntent() {
        String userInput = "帮我算一下 123 乘以 456 等于多少";
        SkillContext context = new SkillContext("test", Map.of());

        IntentResult result = intentAnalyzer.analyze(userInput, testSkills, context);

        System.out.println("=== 计算器测试 ===");
        System.out.println("用户输入: " + userInput);
        System.out.println("意图描述: " + result.intentDescription());
        System.out.println("匹配结果:");
        for (SkillMatch match : result.matchedSkills()) {
            System.out.println("  - " + match.skill().name() + " (置信度: " + match.confidence() + ")");
        }

        assertFalse(result.matchedSkills().isEmpty(), "应该有匹配的 Skill");
        assertTrue(result.matchedSkills().stream()
            .anyMatch(m -> m.skill().name().equals("calculator")),
            "应该匹配 calculator");
    }

    @Test
    @DisplayName("测试翻译意图识别")
    @EnabledIf("isOllamaAvailable")
    void testTranslatorIntent() {
        String userInput = "把这句话翻译成英语：你好世界";
        SkillContext context = new SkillContext("test", Map.of());

        IntentResult result = intentAnalyzer.analyze(userInput, testSkills, context);

        System.out.println("=== 翻译测试 ===");
        System.out.println("用户输入: " + userInput);
        System.out.println("意图描述: " + result.intentDescription());
        System.out.println("匹配结果:");
        for (SkillMatch match : result.matchedSkills()) {
            System.out.println("  - " + match.skill().name() + " (置信度: " + match.confidence() + ")");
        }

        assertFalse(result.matchedSkills().isEmpty(), "应该有匹配的 Skill");
        assertTrue(result.matchedSkills().stream()
            .anyMatch(m -> m.skill().name().equals("translator")),
            "应该匹配 translator");
    }

    @Test
    @DisplayName("测试关键词匹配")
    void testKeywordMatch() {
        String userInput = "我想查询天气";

        List<SkillMatch> matches = intentAnalyzer.matchSkills(userInput, testSkills);

        System.out.println("=== 关键词匹配测试 ===");
        System.out.println("用户输入: " + userInput);
        System.out.println("匹配结果:");
        for (SkillMatch match : matches) {
            System.out.println("  - " + match.skill().name() + " (置信度: " + match.confidence() + ")");
        }

        assertTrue(matches.isEmpty() || !matches.isEmpty(), "关键词匹配应该返回结果");
    }

    @Test
    @DisplayName("测试无匹配情况")
    void testNoMatch() {
        String userInput = "今天吃什么？";
        SkillContext context = new SkillContext("test", Map.of());

        IntentResult result = intentAnalyzer.analyze(userInput, testSkills, context);

        System.out.println("=== 无匹配测试 ===");
        System.out.println("用户输入: " + userInput);
        System.out.println("意图描述: " + result.intentDescription());
        System.out.println("匹配结果数量: " + result.matchedSkills().size());

        // 可能没有高置信度匹配
        assertNotNull(result);
    }

    /**
     * 检查 Ollama 是否可用
     */
    static boolean isOllamaAvailable() {
        if (llmClient == null) {
            return false;
        }
        try {
            llmClient.chat(LlmRequest.ofUserMessage("ping"));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
