package io.github.afgprojects.framework.ai.core.security.dto;

import io.github.afgprojects.framework.ai.core.api.security.PiiDetector;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * PII 检测请求 DTO
 *
 * <p>用于请求检测文本中的个人身份信息。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class PiiCheckRequest {

    /**
     * 待检测的文本内容
     */
    @NotBlank(message = "文本内容不能为空")
    private String text;

    /**
     * 指定检测的 PII 类型
     * <p>
     * 为空则检测所有类型。
     * 可选值：{@link PiiDetector.PiiType} 枚举值，如 PHONE、EMAIL、ID_NUMBER 等。
     */
    private List<String> types;

    /**
     * 脱敏策略
     * <p>
     * 可选值：
     * <ul>
     *   <li>FULL_MASK - 完全替换（如 ***）</li>
     *   <li>PARTIAL_MASK - 部分替换（如 138****1234）</li>
     *   <li>HASH_MASK - 哈希替换</li>
     *   <li>RANDOM_MASK - 随机替换</li>
     *   <li>TYPE_LABEL - 类型标签（如 [PHONE]）</li>
     * </ul>
     * 默认为 PARTIAL_MASK。
     */
    private String maskingStrategy = "PARTIAL_MASK";

    /**
     * 最小置信度阈值
     * <p>
     * 只返回置信度大于等于此值的检测结果，范围 0-1。
     * 默认为 0.7。
     */
    private Double minConfidence;

    /**
     * 用户 ID（可选）
     * <p>
     * 用于审计和上下文关联。
     */
    private String userId;

    /**
     * 租户 ID（可选）
     * <p>
     * 用于多租户场景。
     */
    private String tenantId;
}
