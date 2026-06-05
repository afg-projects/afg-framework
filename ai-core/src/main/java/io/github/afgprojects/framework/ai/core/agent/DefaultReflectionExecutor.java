package io.github.afgprojects.framework.ai.core.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.afgprojects.framework.ai.core.api.chat.AfgChatClient;
import io.github.afgprojects.framework.ai.core.api.chat.AiChatResponse;
import io.github.afgprojects.framework.ai.core.api.chat.AiMessage;
import io.github.afgprojects.framework.ai.core.api.planning.ReflectionExecutor;
import io.github.afgprojects.framework.ai.core.api.planning.ReflectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reflection 执行器默认实现
 *
 * <p>实现 Reflection (反思) 模式，工作流程：
 * <ol>
 *   <li>生成初始响应</li>
 *   <li>对响应进行反思和评估</li>
 *   <li>根据反思改进响应</li>
 *   <li>可选：多轮迭代或质量阈值控制</li>
 * </ol>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultReflectionExecutor implements ReflectionExecutor {

    private static final String REFLECTION_SYSTEM_PROMPT = """
            You are a reflection assistant. Your job is to critically evaluate responses and suggest improvements.

            When evaluating a response, consider:
            1. Correctness - Is the response factually correct?
            2. Completeness - Does it fully address the task?
            3. Clarity - Is the response clear and well-structured?
            4. Quality - Overall quality assessment

            You must respond in JSON format:
            {
              "qualityScore": <number between 0.0 and 1.0>,
              "critique": "<detailed critique of the response>",
              "suggestions": ["<suggestion 1>", "<suggestion 2>"],
              "needsImprovement": <true or false>
            }
            """;

    private static final String IMPROVEMENT_SYSTEM_PROMPT = """
            You are an improvement assistant. Given an original task, a previous response, and reflection feedback,
            generate an improved response that addresses all the issues raised in the reflection.

            Provide a complete, improved response that:
            1. Fixes any errors identified in the reflection
            2. Addresses all suggestions for improvement
            3. Maintains the strengths of the original response
            4. Is clear, complete, and well-structured
            """;

    private final AfgChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public @NonNull ReflectionResult execute(@NonNull String task) {
        log.info("Starting reflection execution for task: {}", task);

        // 1. 生成初始响应
        String initialResponse = generateInitialResponse(task);
        log.debug("Initial response generated");

        // 2. 反思响应
        String reflection = reflect(task, initialResponse);
        log.debug("Reflection completed");

        // 3. 检查是否需要改进
        ReflectionParsedResult parsed = parseReflection(reflection);
        if (!parsed.needsImprovement()) {
            log.info("Response does not need improvement, quality score: {}", parsed.qualityScore());
            return ReflectionResult.success(initialResponse, reflection);
        }

        // 4. 改进响应
        String improvedResponse = improve(task, initialResponse, reflection);
        log.info("Response improved after reflection");

        // 5. 再次反思改进后的响应
        String finalReflection = reflect(task, improvedResponse);

        return ReflectionResult.success(improvedResponse, finalReflection);
    }

    @Override
    public @NonNull ReflectionResult executeWithIterations(@NonNull String task, int iterations) {
        log.info("Starting reflection execution with {} iterations for task: {}", iterations, task);

        if (iterations <= 0) {
            return execute(task);
        }

        // 生成初始响应
        String currentResponse = generateInitialResponse(task);
        String currentReflection = null;

        for (int i = 0; i < iterations; i++) {
            log.debug("Reflection iteration {}/{}", i + 1, iterations);

            // 反思当前响应
            currentReflection = reflect(task, currentResponse);

            // 检查是否需要改进
            ReflectionParsedResult parsed = parseReflection(currentReflection);
            if (!parsed.needsImprovement()) {
                log.info("Response quality sufficient at iteration {}, quality score: {}", i + 1, parsed.qualityScore());
                break;
            }

            // 改进响应
            currentResponse = improve(task, currentResponse, currentReflection);
            log.debug("Response improved at iteration {}", i + 1);
        }

        return ReflectionResult.success(currentResponse, currentReflection);
    }

    @Override
    public @NonNull ReflectionResult executeWithQualityThreshold(
            @NonNull String task,
            double qualityThreshold,
            int maxIterations
    ) {
        log.info("Starting reflection execution with quality threshold {} and max {} iterations for task: {}",
                qualityThreshold, maxIterations, task);

        double effectiveThreshold = qualityThreshold;
        if (effectiveThreshold < 0.0 || effectiveThreshold > 1.0) {
            log.warn("Quality threshold {} out of range [0.0, 1.0], clamping", effectiveThreshold);
            effectiveThreshold = Math.max(0.0, Math.min(1.0, effectiveThreshold));
        }

        // 生成初始响应
        String currentResponse = generateInitialResponse(task);
        String currentReflection = null;
        double currentQuality = 0.0;

        for (int i = 0; i < maxIterations; i++) {
            log.debug("Quality-threshold iteration {}/{}, current quality: {}", i + 1, maxIterations, currentQuality);

            // 反思当前响应
            currentReflection = reflect(task, currentResponse);

            // 解析质量分数
            ReflectionParsedResult parsed = parseReflection(currentReflection);
            currentQuality = parsed.qualityScore();

            log.debug("Quality score after iteration {}: {}", i + 1, currentQuality);

            // 达到质量阈值，停止迭代
            if (currentQuality >= effectiveThreshold) {
                log.info("Quality threshold {} reached at iteration {} with score {}",
                        effectiveThreshold, i + 1, currentQuality);
                break;
            }

            // 改进响应
            currentResponse = improve(task, currentResponse, currentReflection);
            log.debug("Response improved at iteration {}", i + 1);
        }

        if (currentQuality < effectiveThreshold) {
            log.warn("Quality threshold {} not reached after {} iterations (final score: {})",
                    effectiveThreshold, maxIterations, currentQuality);
        }

        return ReflectionResult.success(currentResponse, currentReflection);
    }

    /**
     * 生成初始响应
     */
    private @NonNull String generateInitialResponse(@NonNull String task) {
        AiChatResponse response = chatClient.chat(task);
        return response.content() != null ? response.content() : "";
    }

    /**
     * 对响应进行反思
     */
    private @NonNull String reflect(@NonNull String task, @NonNull String response) {
        String reflectionPrompt = """
                Task: %s

                Response to evaluate:
                %s

                Please evaluate the above response and provide your reflection in JSON format.
                """.formatted(task, response);

        AiChatResponse reflectionResponse = chatClient
                .withSystemPrompt(REFLECTION_SYSTEM_PROMPT)
                .chat(reflectionPrompt);

        return reflectionResponse.content() != null ? reflectionResponse.content() : "";
    }

    /**
     * 根据反思改进响应
     */
    private @NonNull String improve(@NonNull String task, @NonNull String currentResponse, @NonNull String reflection) {
        String improvementPrompt = """
                Original Task: %s

                Previous Response:
                %s

                Reflection Feedback:
                %s

                Please provide an improved response that addresses all the issues raised in the reflection.
                """.formatted(task, currentResponse, reflection);

        AiChatResponse improvedResponse = chatClient
                .withSystemPrompt(IMPROVEMENT_SYSTEM_PROMPT)
                .chat(improvementPrompt);

        return improvedResponse.content() != null ? improvedResponse.content() : currentResponse;
    }

    /**
     * 解析反思结果中的 JSON
     */
    private @NonNull ReflectionParsedResult parseReflection(@NonNull String reflection) {
        try {
            // 尝试从反思文本中提取 JSON
            String json = extractJson(reflection);
            if (json != null) {
                Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<>() {});
                double qualityScore = parsed.containsKey("qualityScore")
                        ? ((Number) parsed.get("qualityScore")).doubleValue()
                        : 0.5;
                boolean needsImprovement = parsed.containsKey("needsImprovement")
                        && Boolean.TRUE.equals(parsed.get("needsImprovement"));
                return new ReflectionParsedResult(qualityScore, needsImprovement);
            }
        } catch (Exception e) {
            log.debug("Failed to parse reflection JSON, using defaults: {}", e.getMessage());
        }

        // JSON 解析失败时的降级逻辑：检查文本中是否包含负面关键词
        boolean needsImprovement = reflection.toLowerCase().contains("incorrect")
                || reflection.toLowerCase().contains("incomplete")
                || reflection.toLowerCase().contains("error")
                || reflection.toLowerCase().contains("needs improvement")
                || reflection.toLowerCase().contains("should be improved");
        return new ReflectionParsedResult(needsImprovement ? 0.3 : 0.7, needsImprovement);
    }

    /**
     * 从文本中提取 JSON 对象
     */
    private String extractJson(@NonNull String text) {
        // 尝试找到 JSON 对象（花括号包围的内容）
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return null;
    }

    /**
     * 解析后的反思结果（内部使用）
     */
    private record ReflectionParsedResult(
            double qualityScore,
            boolean needsImprovement
    ) {}
}
