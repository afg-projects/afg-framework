package io.github.afgprojects.framework.ai.core.dto.agent;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建 Agent 定义请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateAgentRequest {

    @NotBlank(message = "Agent 名称不能为空")
    private String name;

    private String description;

    private String systemPrompt;

    private String chatClientName;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private Integer maxIterations = 10;

    private String knowledgeBaseIds;

    private String toolIds;

    private String config;
}
