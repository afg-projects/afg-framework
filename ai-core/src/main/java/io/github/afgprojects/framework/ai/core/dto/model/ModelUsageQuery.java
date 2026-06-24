package io.github.afgprojects.framework.ai.core.dto.model;

import lombok.Data;

@Data
public class ModelUsageQuery {

    private String modelConfigId;

    private String applicationId;

    private String userId;

    private String status;

    private Integer page = 1;

    private Integer size = 10;
}
