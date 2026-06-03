package io.github.afgprojects.framework.ai.core.dto.resource;

import lombok.Data;

/**
 * 更新应用请求
 *
 * @author afg-projects
 * @since 1.0.0
 */
@Data
public class UpdateApplicationRequest {

    private String name;

    private String description;

    private String type;

    private String accessToken;

    private String status;

    private String config;

    private String icon;

    private Integer sort;
}
