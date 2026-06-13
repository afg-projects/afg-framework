package io.github.afgprojects.framework.core.properties.accesslog;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * 访问日志配置。
 *
 * @since 1.0.0
 */
@Data
public class AfgCoreAccessLogProperties {

    /**
     * 是否启用访问日志。
     */
    private boolean enabled = true;

    /**
     * 排除路径列表（支持 Ant 风格模式）。
     */
    private List<String> excludePaths = new ArrayList<>(List.of("/health", "/actuator/**"));

    /**
     * 是否包含查询字符串。
     */
    private boolean includeQueryString = true;

    /**
     * 是否包含客户端 IP。
     */
    private boolean includeClientIp = true;

    /**
     * 慢请求阈值（毫秒），超过此值将在日志中标记 SLOW。
     */
    private long slowRequestThreshold = 3000;
}
