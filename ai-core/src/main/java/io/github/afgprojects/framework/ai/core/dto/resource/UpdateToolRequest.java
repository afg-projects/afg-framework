package io.github.afgprojects.framework.ai.core.dto.resource;

import lombok.Data;

/**
 * 更新工具请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class UpdateToolRequest {

    private String name;

    private String description;

    private String type;

    private String endpoint;

    private String parameters;

    private String config;

    private Boolean enabled;
}
