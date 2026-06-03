package io.github.afgprojects.framework.ai.core.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.agent.Agent;
import io.github.afgprojects.framework.ai.core.api.agent.AgentExecutor;
import io.github.afgprojects.framework.ai.core.api.agent.AgentRequest;
import io.github.afgprojects.framework.ai.core.api.agent.AgentResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.planning.PlanExecuteExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.PlanExecuteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 默认 Plan-Execute 执行器实现
 *
 * <p>实现 Plan-Execute 模式，工作流程：
 * <ol>
 *   <li>调用 LLM 将目标分解为 JSON 格式的步骤计划</li>
 *   <li>使用 {@link AgentExecutor} 顺序执行每个步骤</li>
 *   <li>支持计划验证和动态重新规划</li>
 * </ol>
 *
 * <p>计划格式（LLM 生成的 JSON）：
 * <pre>{@code
 * {
 *   "steps": [
 *     { "description": "步骤描述", "input": "步骤输入" },
 *     { "description": "步骤描述", "input": "步骤输入" }
 *   ]
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultPlanExecuteExecutor implements PlanExecuteExecutor {

    private static final String PLAN_SYSTEM_PROMPT = """
            You are a planning agent. Given a goal, break it down into a sequence of executable steps.

            You must respond with ONLY a JSON object in the following format (no other text):
            {
              "steps": [
                { "description": "Description of step 1", "input": "Input for step 1" },
                { "description": "Description of step 2", "input": "Input for step 2" }
              ]
            }

            Rules:
            1. Each step should be a clear, actionable task
            2. Steps should be ordered by dependency
            3. Each step's input should be self-contained enough to execute independently
            4. Keep the number of steps minimal while ensuring the goal is fully achieved
            """;

    private static final String VALIDATION_SYSTEM_PROMPT = """
            You are a plan validation agent. Review the following plan and determine if it is feasible and complete.

            Plan:
            %s

            Goal: %s

            Respond with ONLY a JSON object:
            {
              "valid": true/false,
              "reason": "Explanation if invalid, or empty string if valid",
              "suggestedFix": "Suggested improvement if invalid, or empty string if valid"
            }
            """;

    private static final String REPLAN_SYSTEM_PROMPT = """
            You are a replanning agent. The original plan failed at a certain step. Based on the execution results so far, create a new plan to achieve the goal.

            Original goal: %s

            Completed steps and results:
            %s

            Failed step: %s
            Failure reason: %s

            Create a new plan that accounts for what has already been accomplished and the failure.
            Respond with ONLY a JSON object:
            {
              "steps": [
                { "description": "Description of step", "input": "Input for step" }
              ]
            }
            """;

    private static final int DEFAULT_MAX_REPLAN_ATTEMPTS = 3;

    private final AfgChatClient chatClient;
    private final AgentExecutor agentExecutor;
    private final Agent agent;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @NonNull
    public PlanExecuteResult execute(@NonNull String goal) {
        log.info("Starting Plan-Execute for goal: {}", goal);

        // 1. 生成计划
        List<PlanStep> steps = generatePlan(goal);
        if (steps.isEmpty()) {
            log.warn("Generated empty plan for goal: {}", goal);
            return PlanExecuteResult.failure("Failed to generate a valid plan", List.of());
        }

        log.info("Generated plan with {} steps for goal: {}", steps.size(), goal);

        // 2. 顺序执行步骤
        List<Object> stepResults = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            PlanStep step = steps.get(i);
            log.info("Executing step {}/{}: {}", i + 1, steps.size(), step.description());

            try {
                AgentRequest request = new AgentRequest(
                    UUID.randomUUID().toString(),
                    step.input(),
                    Map.of("stepIndex", i, "stepDescription", step.description())
                );

                AgentResponse response = agentExecutor.execute(agent, request);

                if (response.isError()) {
                    log.warn("Step {}/{} failed: {}", i + 1, steps.size(), response.output());
                    return PlanExecuteResult.failure(
                        "Step " + (i + 1) + " failed: " + response.output(),
                        stepResults
                    );
                }

                Object result = response.output();
                stepResults.add(result);
                log.debug("Step {}/{} completed with result: {}", i + 1, steps.size(), result);

            } catch (Exception e) {
                log.error("Step {}/{} threw exception: {}", i + 1, steps.size(), e.getMessage(), e);
                return PlanExecuteResult.failure(
                    "Step " + (i + 1) + " threw exception: " + e.getMessage(),
                    stepResults
                );
            }
        }

        // 3. 汇总最终结果
        String finalResult = buildFinalResult(goal, stepResults);
        log.info("Plan-Execute completed successfully for goal: {}", goal);
        return PlanExecuteResult.success(finalResult, stepResults);
    }

    @Override
    @NonNull
    public PlanExecuteResult executeWithValidation(@NonNull String goal) {
        log.info("Starting Plan-Execute with validation for goal: {}", goal);

        // 1. 生成计划
        List<PlanStep> steps = generatePlan(goal);
        if (steps.isEmpty()) {
            log.warn("Generated empty plan for goal: {}", goal);
            return PlanExecuteResult.failure("Failed to generate a valid plan", List.of());
        }

        // 2. 验证计划
        ValidationResult validation = validatePlan(goal, steps);
        if (!validation.valid()) {
            log.warn("Plan validation failed: {}. Reason: {}", validation.suggestedFix(), validation.reason());

            // 重新生成计划
            log.info("Regenerating plan after validation failure...");
            steps = generatePlan(goal + "\n\nNote: A previous plan was rejected because: " + validation.reason()
                + ". Suggested improvement: " + validation.suggestedFix());

            if (steps.isEmpty()) {
                return PlanExecuteResult.failure("Failed to generate a valid plan after validation", List.of());
            }

            // 再次验证
            ValidationResult revalidation = validatePlan(goal, steps);
            if (!revalidation.valid()) {
                log.warn("Re-generated plan also failed validation: {}", revalidation.reason());
                return PlanExecuteResult.failure(
                    "Plan validation failed even after regeneration: " + revalidation.reason(),
                    List.of()
                );
            }
        }

        log.info("Plan validated with {} steps for goal: {}", steps.size(), goal);

        // 3. 执行已验证的计划
        return executeSteps(goal, steps);
    }

    @Override
    @NonNull
    public PlanExecuteResult executeWithReplanning(@NonNull String goal, boolean allowReplanning) {
        if (!allowReplanning) {
            return execute(goal);
        }

        log.info("Starting Plan-Execute with replanning for goal: {}", goal);

        List<PlanStep> steps = generatePlan(goal);
        if (steps.isEmpty()) {
            return PlanExecuteResult.failure("Failed to generate a valid plan", List.of());
        }

        List<Object> stepResults = new ArrayList<>();
        int replanAttempts = 0;

        int stepIndex = 0;
        while (stepIndex < steps.size()) {
            PlanStep step = steps.get(stepIndex);
            log.info("Executing step {}/{}: {}", stepIndex + 1, steps.size(), step.description());

            try {
                AgentRequest request = new AgentRequest(
                    UUID.randomUUID().toString(),
                    step.input(),
                    Map.of("stepIndex", stepIndex, "stepDescription", step.description())
                );

                AgentResponse response = agentExecutor.execute(agent, request);

                if (response.isError()) {
                    log.warn("Step {}/{} failed: {}", stepIndex + 1, steps.size(), response.output());

                    if (replanAttempts >= DEFAULT_MAX_REPLAN_ATTEMPTS) {
                        log.warn("Max replan attempts ({}) reached, returning failure", DEFAULT_MAX_REPLAN_ATTEMPTS);
                        return PlanExecuteResult.failure(
                            "Step " + (stepIndex + 1) + " failed and max replan attempts reached: " + response.output(),
                            stepResults
                        );
                    }

                    // 重新规划
                    replanAttempts++;
                    log.info("Replanning (attempt {}/{}) after step {} failure...",
                        replanAttempts, DEFAULT_MAX_REPLAN_ATTEMPTS, stepIndex + 1);

                    List<PlanStep> newSteps = replan(goal, stepResults, step, response.output());
                    if (newSteps.isEmpty()) {
                        log.warn("Replanning produced empty plan, returning failure");
                        return PlanExecuteResult.failure(
                            "Step " + (stepIndex + 1) + " failed and replanning produced no new plan",
                            stepResults
                        );
                    }

                    // 用新计划替换剩余步骤
                    steps = newSteps;
                    stepIndex = 0;
                    log.info("Replanned with {} new steps", steps.size());
                    continue;
                }

                Object result = response.output();
                stepResults.add(result);
                stepIndex++;

            } catch (Exception e) {
                log.error("Step {}/{} threw exception: {}", stepIndex + 1, steps.size(), e.getMessage(), e);

                if (replanAttempts >= DEFAULT_MAX_REPLAN_ATTEMPTS) {
                    return PlanExecuteResult.failure(
                        "Step " + (stepIndex + 1) + " threw exception and max replan attempts reached: " + e.getMessage(),
                        stepResults
                    );
                }

                replanAttempts++;
                log.info("Replanning (attempt {}/{}) after step {} exception...",
                    replanAttempts, DEFAULT_MAX_REPLAN_ATTEMPTS, stepIndex + 1);

                List<PlanStep> newSteps = replan(goal, stepResults, steps.get(stepIndex), e.getMessage());
                if (newSteps.isEmpty()) {
                    return PlanExecuteResult.failure(
                        "Step " + (stepIndex + 1) + " threw exception and replanning produced no new plan",
                        stepResults
                    );
                }

                steps = newSteps;
                stepIndex = 0;
                log.info("Replanned with {} new steps", steps.size());
            }
        }

        String finalResult = buildFinalResult(goal, stepResults);
        log.info("Plan-Execute with replanning completed successfully for goal: {}", goal);
        return PlanExecuteResult.success(finalResult, stepResults);
    }

    // ---- 内部方法 ----

    /**
     * 调用 LLM 生成执行计划
     */
    private @NonNull List<PlanStep> generatePlan(@NonNull String goal) {
        log.debug("Generating plan for goal: {}", goal);

        try {
            AiChatResponse response = chatClient
                .withSystemPrompt(PLAN_SYSTEM_PROMPT)
                .chat(goal);

            String content = response.content() != null ? response.content() : "";
            return parsePlan(content);
        } catch (Exception e) {
            log.error("Failed to generate plan: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 验证计划的可行性
     */
    private @NonNull ValidationResult validatePlan(@NonNull String goal, @NonNull List<PlanStep> steps) {
        log.debug("Validating plan with {} steps for goal: {}", steps.size(), goal);

        try {
            String planDescription = formatStepsForPrompt(steps);
            String validationPrompt = String.format(VALIDATION_SYSTEM_PROMPT, planDescription, goal);

            AiChatResponse response = chatClient.chat(validationPrompt);
            String content = response.content() != null ? response.content() : "";

            return parseValidationResult(content);
        } catch (Exception e) {
            log.warn("Plan validation failed with exception, treating as valid: {}", e.getMessage());
            return new ValidationResult(true, "", "");
        }
    }

    /**
     * 重新规划
     */
    private @NonNull List<PlanStep> replan(
        @NonNull String goal,
        @NonNull List<Object> completedResults,
        @NonNull PlanStep failedStep,
        @NonNull String failureReason
    ) {
        log.debug("Replanning for goal: {}", goal);

        try {
            String completedDescription = formatCompletedSteps(completedResults);
            String replanPrompt = String.format(
                REPLAN_SYSTEM_PROMPT,
                goal,
                completedDescription,
                failedStep.description(),
                failureReason
            );

            AiChatResponse response = chatClient
                .withSystemPrompt("You are a replanning agent. Create a new plan based on the context provided.")
                .chat(replanPrompt);

            String content = response.content() != null ? response.content() : "";
            return parsePlan(content);
        } catch (Exception e) {
            log.error("Replanning failed: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 顺序执行步骤（内部方法，供 executeWithValidation 使用）
     */
    private @NonNull PlanExecuteResult executeSteps(@NonNull String goal, @NonNull List<PlanStep> steps) {
        List<Object> stepResults = new ArrayList<>();

        for (int i = 0; i < steps.size(); i++) {
            PlanStep step = steps.get(i);
            log.info("Executing step {}/{}: {}", i + 1, steps.size(), step.description());

            try {
                AgentRequest request = new AgentRequest(
                    UUID.randomUUID().toString(),
                    step.input(),
                    Map.of("stepIndex", i, "stepDescription", step.description())
                );

                AgentResponse response = agentExecutor.execute(agent, request);

                if (response.isError()) {
                    log.warn("Step {}/{} failed: {}", i + 1, steps.size(), response.output());
                    return PlanExecuteResult.failure(
                        "Step " + (i + 1) + " failed: " + response.output(),
                        stepResults
                    );
                }

                Object result = response.output();
                stepResults.add(result);

            } catch (Exception e) {
                log.error("Step {}/{} threw exception: {}", i + 1, steps.size(), e.getMessage(), e);
                return PlanExecuteResult.failure(
                    "Step " + (i + 1) + " threw exception: " + e.getMessage(),
                    stepResults
                );
            }
        }

        String finalResult = buildFinalResult(goal, stepResults);
        return PlanExecuteResult.success(finalResult, stepResults);
    }

    /**
     * 解析 LLM 返回的 JSON 计划
     */
    private @NonNull List<PlanStep> parsePlan(@NonNull String content) {
        String json = extractJson(content);
        if (json == null) {
            log.warn("No JSON found in plan response");
            return List.of();
        }

        try {
            Map<String, Object> planMap = objectMapper.readValue(json, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<Map<String, String>> stepsList = (List<Map<String, String>>) planMap.get("steps");

            if (stepsList == null || stepsList.isEmpty()) {
                log.warn("Plan JSON has no steps");
                return List.of();
            }

            List<PlanStep> steps = new ArrayList<>();
            for (Map<String, String> stepMap : stepsList) {
                String description = stepMap.getOrDefault("description", "");
                String input = stepMap.getOrDefault("input", "");
                if (!description.isBlank()) {
                    steps.add(new PlanStep(description, input));
                }
            }

            return steps;
        } catch (Exception e) {
            log.error("Failed to parse plan JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析验证结果
     */
    private @NonNull ValidationResult parseValidationResult(@NonNull String content) {
        String json = extractJson(content);
        if (json == null) {
            log.warn("No JSON found in validation response, treating as valid");
            return new ValidationResult(true, "", "");
        }

        try {
            Map<String, Object> resultMap = objectMapper.readValue(json, new TypeReference<>() {});
            boolean valid = Boolean.TRUE.equals(resultMap.get("valid"));
            String reason = (String) resultMap.getOrDefault("reason", "");
            String suggestedFix = (String) resultMap.getOrDefault("suggestedFix", "");
            return new ValidationResult(valid, reason, suggestedFix);
        } catch (Exception e) {
            log.warn("Failed to parse validation JSON, treating as valid: {}", e.getMessage());
            return new ValidationResult(true, "", "");
        }
    }

    /**
     * 从 LLM 响应中提取 JSON 内容
     */
    private String extractJson(@NonNull String content) {
        // 尝试提取 ```json ... ``` 代码块
        int jsonBlockStart = content.indexOf("```json");
        if (jsonBlockStart >= 0) {
            int jsonStart = content.indexOf('\n', jsonBlockStart) + 1;
            int jsonEnd = content.indexOf("```", jsonStart);
            if (jsonEnd > jsonStart) {
                return content.substring(jsonStart, jsonEnd).trim();
            }
        }

        // 尝试提取 ``` ... ``` 代码块
        int codeBlockStart = content.indexOf("```");
        if (codeBlockStart >= 0) {
            int codeStart = content.indexOf('\n', codeBlockStart) + 1;
            int codeEnd = content.indexOf("```", codeStart);
            if (codeEnd > codeStart) {
                return content.substring(codeStart, codeEnd).trim();
            }
        }

        // 尝试直接查找 JSON 对象
        int braceStart = content.indexOf('{');
        int braceEnd = content.lastIndexOf('}');
        if (braceStart >= 0 && braceEnd > braceStart) {
            return content.substring(braceStart, braceEnd + 1).trim();
        }

        return null;
    }

    /**
     * 格式化步骤列表用于提示词
     */
    private @NonNull String formatStepsForPrompt(@NonNull List<PlanStep> steps) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.size(); i++) {
            PlanStep step = steps.get(i);
            sb.append(i + 1).append(". ").append(step.description());
            if (!step.input().isBlank()) {
                sb.append(" (Input: ").append(step.input()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 格式化已完成步骤的结果用于重新规划
     */
    private @NonNull String formatCompletedSteps(@NonNull List<Object> results) {
        if (results.isEmpty()) {
            return "No steps completed yet.";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append("Step ").append(i + 1).append(" result: ").append(results.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 构建最终结果描述
     */
    private @NonNull String buildFinalResult(@NonNull String goal, @NonNull List<Object> stepResults) {
        if (stepResults.isEmpty()) {
            return "Goal: " + goal + " - No steps were executed";
        }

        Object lastResult = stepResults.get(stepResults.size() - 1);
        return lastResult != null ? lastResult.toString() : "Goal: " + goal + " - Completed with null final result";
    }

    // ---- 内部记录 ----

    /**
     * 计划步骤
     */
    record PlanStep(String description, String input) {}

    /**
     * 验证结果
     */
    record ValidationResult(boolean valid, String reason, String suggestedFix) {}
}
