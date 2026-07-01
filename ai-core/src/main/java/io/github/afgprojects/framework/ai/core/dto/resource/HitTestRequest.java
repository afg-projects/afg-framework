package io.github.afgprojects.framework.ai.core.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 应用命中测试请求
 *
 * <p>输入问题，对应用关联的知识库与工具进行命中测试，返回匹配结果。</p>
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class HitTestRequest {

    @NotBlank(message = "测试问题不能为空")
    private String question;

    /** 检索模式（透传给知识库检索，可选） */
    private String searchMode;

    /** 相似度阈值（透传给知识库检索，可选） */
    private Double similarityThreshold;

    /** 返回条数上限（透传给知识库检索 topK，可选） */
    private Integer topN;
}
