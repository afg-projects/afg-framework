package io.github.afgprojects.framework.ai.core.skill;

import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.skill.*;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.List;

/**
 * 默认 Skill 调度器实现
 *
 * <p>负责根据意图分析结果调度执行 Skill。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
public class DefaultSkillDispatcher implements SkillDispatcher {

    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;

    private final IntentAnalyzer intentAnalyzer;
    private final SkillExecutor skillExecutor;
    private final SkillRegistry skillRegistry;
    private final AfgChatClient chatClient;
    private final double confidenceThreshold;

    public DefaultSkillDispatcher(
            @NonNull IntentAnalyzer intentAnalyzer,
            @NonNull SkillExecutor skillExecutor,
            @NonNull SkillRegistry skillRegistry,
            @NonNull AfgChatClient chatClient
    ) {
        this(intentAnalyzer, skillExecutor, skillRegistry, chatClient, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    public DefaultSkillDispatcher(
            @NonNull IntentAnalyzer intentAnalyzer,
            @NonNull SkillExecutor skillExecutor,
            @NonNull SkillRegistry skillRegistry,
            @NonNull AfgChatClient chatClient,
            double confidenceThreshold
    ) {
        this.intentAnalyzer = intentAnalyzer;
        this.skillExecutor = skillExecutor;
        this.skillRegistry = skillRegistry;
        this.chatClient = chatClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    @NonNull
    public SkillRoutingResult dispatch(@NonNull String input) {
        log.info("Dispatching user input: {}", input);

        // 1. 获取所有可用 Skills
        List<SkillDefinition> availableSkills = skillRegistry.getAll();

        if (availableSkills.isEmpty()) {
            log.warn("No skills available, falling back to LLM");
            return handleWithLlm(input);
        }

        // 2. 分析意图
        IntentResult intent = intentAnalyzer.analyze(input);

        log.debug("Intent analysis result: {} matches", intent.matches().size());

        // 3. 检查是否有匹配
        if (!intent.hasMatch()) {
            log.info("No skill matched, falling back to LLM");
            return handleWithLlm(input);
        }

        // 4. 获取最佳匹配
        SkillMatch bestMatch = intent.bestMatch();

        // 5. 检查置信度
        if (bestMatch.confidence() < confidenceThreshold) {
            log.info("Skill confidence too low: {}, falling back to LLM", bestMatch.confidence());
            return SkillRoutingResult.notMatched(
                    "置信度过低（" + bestMatch.confidence() + "），建议用户确认或使用通用回答"
            );
        }

        // 6. 执行推荐的 Skill
        log.info("Executing skill: {} with confidence: {}", bestMatch.skill().name(), bestMatch.confidence());

        try {
            SkillContext executionContext = SkillContext.builder(bestMatch.skill().name())
                    .inputs(bestMatch.parameters())
                    .build();

            SkillResult result = skillExecutor.execute(executionContext);

            if (result.isSuccess()) {
                return SkillRoutingResult.matched(bestMatch.skill(), bestMatch.confidence());
            } else {
                return SkillRoutingResult.notMatched(result.getErrorMessage());
            }

        } catch (Exception e) {
            log.error("Skill execution failed: {}", e.getMessage());
            return SkillRoutingResult.notMatched(e.getMessage());
        }
    }

    @Override
    @NonNull
    public IntentAnalyzer getIntentAnalyzer() {
        return intentAnalyzer;
    }

    @Override
    @NonNull
    public SkillRegistry getRegistry() {
        return skillRegistry;
    }

    /**
     * 使用 LLM 直接处理（无 Skill 匹配时）
     */
    private SkillRoutingResult handleWithLlm(String input) {
        AiChatResponse response = chatClient
                .withSystemPrompt("你是一个智能助手，请根据用户的问题提供帮助。")
                .chat(input);

        return SkillRoutingResult.notMatched("使用 LLM 直接回答: " + response.content());
    }
}