package io.github.afgprojects.framework.core.properties.logging;

import lombok.Data;

/**
 * 日志文件配置。
 */
@Data
public class AfgCoreLoggingFileProperties {

    private String path = "./logs";
    private String maxSize = "100MB";
    private int maxHistory = 30;
    private String totalSizeCap = "10GB";
}
