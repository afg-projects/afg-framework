package io.github.afgprojects.framework.ai.core.properties.etl;

import lombok.Data;

/**
 * 文档分割配置。
 */
@Data
public class SplitterConfig {

    /**
     * 分块大小（字符数）。
     */
    private int chunkSize = 500;

    /**
     * 分块重叠（字符数）。
     */
    private int chunkOverlap = 50;
}
