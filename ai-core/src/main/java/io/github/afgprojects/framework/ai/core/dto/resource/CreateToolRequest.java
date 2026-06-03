package io.github.afgprojects.framework.ai.core.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建工具请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateToolRequest {

    @NotBlank(message = "工具名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "工具类型不能为空")
    private String type;

    private String endpoint;

    private String parameters;

    private String config;

    private Boolean enabled = true;
}
