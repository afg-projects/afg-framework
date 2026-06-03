package io.github.afgprojects.framework.ai.core.dto.resource;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建应用请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class CreateApplicationRequest {

    @NotBlank(message = "应用名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "应用类型不能为空")
    private String type;

    private String accessToken;

    private String status = "ACTIVE";

    private String config;

    private String icon;

    private Integer sort = 0;
}
