package io.github.afgprojects.framework.ai.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * PII 检测响应 DTO
 *
 * <p>返回 PII 检测结果，包括检测到的 PII 详情和脱敏后的文本。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PiiCheckResponse {

    /**
     * 是否包含 PII
     */
    private boolean containsPii;

    /**
     * 检测到的 PII 数量
     */
    private int piiCount;

    /**
     * 原始文本
     */
    private String originalText;

    /**
     * 脱敏后的文本
     */
    private String maskedText;

    /**
     * 检测到的 PII 详情列表
     */
    private List<PiiDetection> detections;

    /**
     * 风险等级
     * <p>
     * 可选值：NONE、LOW、MEDIUM、HIGH、CRITICAL
     */
    private String riskLevel;

    /**
     * PII 检测详情
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PiiDetection {

        /**
         * PII 类型
         * <p>
         * 如：PHONE、EMAIL、ID_NUMBER 等
         */
        private String type;

        /**
         * 原始值
         */
        private String originalValue;

        /**
         * 脱敏后的值
         */
        private String maskedValue;

        /**
         * 在文本中的起始位置
         */
        private int startIndex;

        /**
         * 在文本中的结束位置
         */
        private int endIndex;

        /**
         * 检测置信度（0-1）
         */
        private double confidence;
    }
}
