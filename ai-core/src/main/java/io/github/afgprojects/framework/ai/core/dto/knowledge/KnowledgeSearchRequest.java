package io.github.afgprojects.framework.ai.core.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 知识库搜索请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class KnowledgeSearchRequest {

    @NotNull(message = "知识库ID列表不能为空")
    private List<String> knowledgeBaseIds;

    @NotBlank(message = "搜索查询不能为空")
    private String query;

    private int topK = 5;

    private double similarityThreshold = 0.7;
}
