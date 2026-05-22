package io.github.afgprojects.framework.ai.agent.skill.dispatcher;

import io.github.afgprojects.framework.ai.agent.skill.SkillContext;
import io.github.afgprojects.framework.ai.agent.skill.SkillDefinition;
import io.github.afgprojects.framework.ai.agent.skill.SkillExecutor;
import io.github.afgprojects.framework.ai.agent.skill.SkillRegistry;
import io.github.afgprojects.framework.ai.agent.skill.SkillResult;
import io.github.afgprojects.framework.ai.agent.skill.intent.IntentAnalyzer;
import io.github.afgprojects.framework.ai.agent.skill.intent.IntentResult;
import io.github.afgprojects.framework.ai.agent.skill.intent.SkillMatch;
import io.github.afgprojects.framework.ai.core.model.LlmClient;
import io.github.afgprojects.framework.ai.core.model.LlmRequest;
import io.github.afgprojects.framework.ai.core.model.LlmResponse;
import io.github.afgprojects.framework.ai.core.tool.ToolDefinition;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 默认 Skill 调度器实现
 *
 * <p>负责根据意图分析结果调度执行 Skill，支持澄清、回退到通用 LLM 处理等。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultSkillDispatcher implements SkillDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultSkillDispatcher.class);

    private static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.5;

    private final IntentAnalyzer intentAnalyzer;
    private final SkillExecutor skillExecutor;
    private final SkillRegistry skillRegistry;
    private final LlmClient llmClient;
    private final double confidenceThreshold;

    public DefaultSkillDispatcher(
        @NonNull IntentAnalyzer intentAnalyzer,
        @NonNull SkillExecutor skillExecutor,
        @NonNull SkillRegistry skillRegistry,
        @NonNull LlmClient llmClient
    ) {
        this(intentAnalyzer, skillExecutor, skillRegistry, llmClient, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    public DefaultSkillDispatcher(
        @NonNull IntentAnalyzer intentAnalyzer,
        @NonNull SkillExecutor skillExecutor,
        @NonNull SkillRegistry skillRegistry,
        @NonNull LlmClient llmClient,
        double confidenceThreshold
    ) {
        this.intentAnalyzer = intentAnalyzer;
        this.skillExecutor = skillExecutor;
        this.skillRegistry = skillRegistry;
        this.llmClient = llmClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    @Override
    @NonNull
    public SkillRoutingResult dispatch(@NonNull String userInput, @NonNull SkillContext context) {
        log.info("Dispatching user input: {}", userInput);

        // 1. 获取所有可用 Skills
        List<SkillDefinition> availableSkills = skillRegistry.getAll();

        if (availableSkills.isEmpty()) {
            log.warn("No skills available, falling back to LLM");
            return handleWithLlm(userInput, context);
        }

        // 2. 分析意图
        IntentResult intent = intentAnalyzer.analyze(userInput, availableSkills, context);

        log.debug("Intent analysis result: {} matches, needs clarification: {}",
            intent.matchedSkills().size(), intent.needsClarification());

        // 3. 检查是否需要澄清
        if (intent.needsClarification()) {
            return SkillRoutingResult.needsClarification(
                intent.clarificationQuestion(),
                intent.matchedSkills()
            );
        }

        // 4. 检查是否有推荐 Skill
        if (intent.recommendedSkill() == null) {
            log.info("No skill matched, falling back to LLM");
            return handleWithLlm(userInput, context);
        }

        SkillMatch recommended = intent.recommendedSkill();

        // 5. 检查置信度
        if (recommended.confidence() < confidenceThreshold) {
            log.info("Skill confidence too low: {}, falling back to LLM", recommended.confidence());
            return SkillRoutingResult.lowConfidence(
                intent.matchedSkills(),
                "置信度过低（" + recommended.confidence() + "），建议用户确认或使用通用回答"
            );
        }

        // 6. 执行推荐的 Skill
        log.info("Executing skill: {} with confidence: {}", recommended.skill().name(), recommended.confidence());

        SkillContext executionContext = context.createChild(
            recommended.skill().name(),
            intent.extractedParameters()
        );

        try {
            SkillResult result = skillExecutor.execute(executionContext);

            if (result.success()) {
                // 如果 Skill 执行成功，可能需要进一步用 LLM 处理
                String finalOutput = processSkillOutput(result, userInput, context);
                return SkillRoutingResult.success(
                    new SkillResult(true, finalOutput, null, result.toolCalls(), result.subSkills(), result.metadata()),
                    recommended
                );
            } else {
                return SkillRoutingResult.executionFailed(result.error());
            }

        } catch (Exception e) {
            log.error("Skill execution failed: {}", e.getMessage());
            return SkillRoutingResult.executionFailed(e.getMessage());
        }
    }

    @Override
    @NonNull
    public List<SkillResult> dispatchMultiple(
        @NonNull String userInput,
        @NonNull List<SkillDefinition> skills,
        @NonNull SkillContext context,
        @NonNull DispatchStrategy strategy
    ) {
        List<SkillResult> results = new ArrayList<>();

        switch (strategy) {
            case PARALLEL -> {
                List<CompletableFuture<SkillResult>> futures = skills.stream()
                    .map(skill -> CompletableFuture.supplyAsync(() -> {
                        SkillContext skillContext = context.createChild(skill.name(), Map.of());
                        return skillExecutor.execute(skillContext);
                    }))
                    .toList();

                results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            }

            case SEQUENTIAL -> {
                for (SkillDefinition skill : skills) {
                    SkillContext skillContext = context.createChild(skill.name(), Map.of());
                    SkillResult result = skillExecutor.execute(skillContext);
                    results.add(result);
                }
            }

            case CONDITIONAL -> {
                // 条件执行：根据前一个结果决定是否继续
                for (int i = 0; i < skills.size(); i++) {
                    SkillDefinition skill = skills.get(i);
                    SkillContext skillContext = context.createChild(skill.name(), Map.of());
                    SkillResult result = skillExecutor.execute(skillContext);
                    results.add(result);

                    // 如果失败，停止后续执行
                    if (!result.success()) {
                        break;
                    }

                    // 将结果传递给下一个 Skill
                    if (i < skills.size() - 1 && result.output() != null) {
                        context.setVariable("previousResult", result.output());
                    }
                }
            }
        }

        return results;
    }

    /**
     * 使用 LLM 直接处理（无 Skill 匹配时）
     */
    private SkillRoutingResult handleWithLlm(String userInput, SkillContext context) {
        LlmRequest request = LlmRequest.builder()
            .systemPrompt("你是一个智能助手，请根据用户的问题提供帮助。")
            .addMessage(io.github.afgprojects.framework.ai.core.memory.Message.user(userInput))
            .build();

        LlmResponse response = llmClient.chat(request);

        SkillResult llmResult = SkillResult.success(response.content());
        return new SkillRoutingResult(
            SkillRoutingResult.RoutingStatus.SUCCESS,
            llmResult,
            null,
            List.of(),
            null,
            "使用 LLM 直接回答"
        );
    }

    /**
     * 处理 Skill 输出，可能需要用 LLM 进一步处理
     */
    private String processSkillOutput(SkillResult result, String userInput, SkillContext context) {
        // 如果 Skill 返回的是渲染后的提示词，需要用 LLM 执行
        if (result.metadata() != null && result.metadata().containsKey("prompt")) {
            String prompt = (String) result.metadata().get("prompt");

            log.debug("Executing skill prompt with LLM: {}", prompt);

            LlmRequest request = LlmRequest.builder()
                .systemPrompt("你是一个智能助手，请根据提示完成任务。")
                .addMessage(io.github.afgprojects.framework.ai.core.memory.Message.user(prompt))
                .build();

            LlmResponse response = llmClient.chat(request);
            return response.content();
        }

        // 否则直接返回输出
        return result.output();
    }
}