package io.github.afgprojects.framework.core.properties.logging;

import lombok.Data;

/**
 * 日志配置。
 */
@Data
public class AfgCoreLoggingProperties {

    /**
     * 是否启用敏感信息脱敏。
     */
    private boolean maskSensitive = true;

    /**
     * MDC 配置。
     */
    private AfgCoreLoggingMdcProperties mdc = new AfgCoreLoggingMdcProperties();

    /**
     * 结构化日志配置。
     */
    private AfgCoreLoggingStructuredProperties structured = new AfgCoreLoggingStructuredProperties();

    /**
     * 日志文件配置。
     */
    private AfgCoreLoggingFileProperties file = new AfgCoreLoggingFileProperties();

    /**
     * 异步日志配置。
     */
    private AfgCoreLoggingAsyncProperties async = new AfgCoreLoggingAsyncProperties();
}
