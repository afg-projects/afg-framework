package io.github.afgprojects.framework.ai.core.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建知识库请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateKnowledgeBaseRequest {

    @NotBlank(message = "知识库名称不能为空")
    private String name;

    private String description;

    private String embeddingModelName;

    private String config;
}
