package io.github.afgprojects.framework.ai.core.dto.model;

import lombok.Data;

@Data
public class UpdateModelConfigRequest {

    private String modelName;

    private String displayName;

    private String modelType;

    private String capabilities;

    private String config;

    private Boolean enabled;
}
