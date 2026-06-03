package io.github.afgprojects.framework.ai.core.properties.etl;

import lombok.Data;

/**
 * ETL 配置。
 */
@Data
public class EtlConfig {

    /**
     * 是否启用 ETL。
     */
    private boolean enabled = true;

    /**
     * 文档分割配置。
     */
    private SplitterConfig splitter = new SplitterConfig();
}
