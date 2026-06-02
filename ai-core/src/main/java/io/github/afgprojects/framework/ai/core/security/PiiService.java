package io.github.afgprojects.framework.ai.core.security;

import io.github.afgprojects.framework.ai.core.api.security.ContentSafetyChecker;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import io.github.afgprojects.framework.ai.core.api.security.PiiDetector.*;
import io.github.afgprojects.framework.ai.core.security.dto.PiiCheckRequest;
import io.github.afgprojects.framework.ai.core.security.dto.PiiCheckResponse;
import io.github.afgprojects.framework.ai.core.security.dto.PiiCheckResponse.PiiDetection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * PII 检测服务
 *
 * <p>封装 PII 检测/脱敏通用逻辑，提供简洁的 API 供上层使用。
 * 内部委托给框架 {@link PiiDetector} 实现。
 *
 * <p>使用示例：
 * <pre>{@code
 * @RestController
 * @RequiredArgsConstructor
 * public class PiiController {
 *     private final PiiService piiService;
 *
 *     @PostMapping("/check")
 *     public PiiCheckResponse check(@RequestBody PiiCheckRequest request) {
 *         return piiService.check(request);
 *     }
 * }
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PiiService {

    private final PiiDetector piiDetector;

    /**
     * 检测文本中的 PII
     *
     * <p>根据请求参数执行 PII 检测，返回包含检测结果和脱敏文本的响应。
     *
     * @param request PII 检测请求
     * @return PII 检测响应
     */
    @NonNull
    public PiiCheckResponse check(@NonNull PiiCheckRequest request) {
        log.debug("PII check: text length={}, types={}", request.getText().length(), request.getTypes());

        PiiContext context = createPiiContext(request);
        PiiDetectionResult result = piiDetector.detect(request.getText(), context);

        return toResponse(request.getText(), result);
    }

    /**
     * 检测文本中的 PII（指定上下文）
     *
     * @param text    待检测文本
     * @param context 检测上下文
     * @return PII 检测响应
     */
    @NonNull
    public PiiCheckResponse check(@NonNull String text, @NonNull PiiContext context) {
        log.debug("PII check with context: text length={}", text.length());

        PiiDetectionResult result = piiDetector.detect(text, context);

        return toResponse(text, result);
    }

    /**
     * 快速检测文本是否包含 PII
     *
     * @param text 待检测文本
     * @return 是否包含 PII
     */
    public boolean containsPii(@NonNull String text) {
        log.debug("Quick PII check: text length={}", text.length());

        PiiContext context = createDefaultPiiContext();
        PiiDetectionResult result = piiDetector.detect(text, context);

        return result.hasPii();
    }

    /**
     * 脱敏文本中的 PII
     *
     * <p>使用默认脱敏策略（PARTIAL_MASK）进行脱敏。
     *
     * @param text 待脱敏文本
     * @return 脱敏后的文本
     */
    @NonNull
    public String mask(@NonNull String text) {
        return mask(text, MaskingStrategy.PARTIAL_MASK);
    }

    /**
     * 脱敏文本中的 PII（指定策略）
     *
     * @param text     待脱敏文本
     * @param strategy 脱敏策略
     * @return 脱敏后的文本
     */
    @NonNull
    public String mask(@NonNull String text, @NonNull MaskingStrategy strategy) {
        log.debug("Mask PII: text length={}, strategy={}", text.length(), strategy);

        PiiContext context = new SimplePiiContext(null, null, Collections.emptyList(), strategy, 0.7);
        PiiMaskingResult result = piiDetector.mask(text, context);

        return result.getMaskedText();
    }

    /**
     * 还原脱敏内容
     *
     * @param maskedText 脱敏后的文本
     * @param token      脱敏令牌（包含原始值映射）
     * @return 还原后的文本
     */
    @NonNull
    public String unmask(@NonNull String maskedText, @NonNull MaskingToken token) {
        log.debug("Unmask PII: text length={}", maskedText.length());

        return piiDetector.unmask(maskedText, token);
    }

    // ========== 私有方法 ==========

    private PiiContext createPiiContext(PiiCheckRequest request) {
        List<PiiType> detectTypes = parsePiiTypes(request.getTypes());
        MaskingStrategy strategy = parseMaskingStrategy(request.getMaskingStrategy());
        double minConfidence = request.getMinConfidence() != null ? request.getMinConfidence() : 0.7;

        return new SimplePiiContext(
                request.getUserId(),
                request.getTenantId(),
                detectTypes,
                strategy,
                minConfidence
        );
    }

    private PiiContext createDefaultPiiContext() {
        return new SimplePiiContext(null, null, Collections.emptyList(), MaskingStrategy.PARTIAL_MASK, 0.7);
    }

    private List<PiiType> parsePiiTypes(List<String> typeNames) {
        if (typeNames == null || typeNames.isEmpty()) {
            return Collections.emptyList();
        }

        List<PiiType> result = new ArrayList<>();
        for (String typeName : typeNames) {
            try {
                result.add(PiiType.valueOf(typeName.toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown PII type: {}, ignoring", typeName);
            }
        }

        return result;
    }

    private MaskingStrategy parseMaskingStrategy(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            return MaskingStrategy.PARTIAL_MASK;
        }

        try {
            return MaskingStrategy.valueOf(strategy.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown masking strategy: {}, falling back to PARTIAL_MASK", strategy);
            return MaskingStrategy.PARTIAL_MASK;
        }
    }

    private PiiCheckResponse toResponse(String text, PiiDetectionResult result) {
        List<PiiDetection> detections = new ArrayList<>();

        for (PiiMatch match : result.getPiiMatches()) {
            detections.add(PiiDetection.builder()
                    .type(match.getType().name())
                    .originalValue(match.getText())
                    .maskedValue(match.getMaskedValue())
                    .startIndex(match.getStart())
                    .endIndex(match.getEnd())
                    .confidence(match.getConfidence())
                    .build());
        }

        return PiiCheckResponse.builder()
                .containsPii(result.hasPii())
                .piiCount(detections.size())
                .originalText(text)
                .detections(detections)
                .riskLevel(result.getRiskLevel().name())
                .build();
    }

    /**
     * 简单的 PII 上下文实现
     */
    private static class SimplePiiContext implements PiiContext {
        private final String userId;
        private final String tenantId;
        private final List<PiiType> detectTypes;
        private final MaskingStrategy maskingStrategy;
        private final double minConfidence;

        SimplePiiContext(String userId, String tenantId, List<PiiType> detectTypes,
                         MaskingStrategy maskingStrategy, double minConfidence) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.detectTypes = detectTypes;
            this.maskingStrategy = maskingStrategy;
            this.minConfidence = minConfidence;
        }

        @Override
        public String getUserId() { return userId; }

        @Override
        public String getTenantId() { return tenantId; }

        @Override
        @NonNull
        public List<PiiType> getDetectTypes() { return detectTypes; }

        @Override
        @NonNull
        public MaskingStrategy getMaskingStrategy() { return maskingStrategy; }

        @Override
        public double getMinConfidence() { return minConfidence; }
    }
}
