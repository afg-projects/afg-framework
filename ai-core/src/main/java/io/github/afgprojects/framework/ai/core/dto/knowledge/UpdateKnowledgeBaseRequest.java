package io.github.afgprojects.framework.ai.core.dto.knowledge;

import lombok.Data;

/**
 * 更新知识库请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class UpdateKnowledgeBaseRequest {

    private String name;

    private String description;

    private String embeddingModelName;

    private String config;
}
