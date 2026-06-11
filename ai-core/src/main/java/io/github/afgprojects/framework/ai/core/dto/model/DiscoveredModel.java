package io.github.afgprojects.framework.ai.core.dto.model;

import lombok.Builder;
import lombok.Data;

/**
 * 从供应商 API 发现的模型信息
 */
@Data
@Builder
public class DiscoveredModel {
    private String modelName;
    private String modelType;
    private String displayName;
    private Integer contextWindow;
    private Integer dimensions;
    private String ownedBy;
}
