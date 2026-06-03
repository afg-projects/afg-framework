package io.github.afgprojects.framework.ai.core.dto.agent;

import lombok.Data;

/**
 * 更新 Agent 定义请求
 * <p>
 * 所有字段均为可选，仅更新非 null 字段。
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class UpdateAgentRequest {

    private String name;

    private String description;

    private String systemPrompt;

    private String chatClientName;

    private String modelName;

    private Double temperature;

    private Integer maxTokens;

    private Integer maxIterations;

    private String knowledgeBaseIds;

    private String toolIds;

    private String config;
}
