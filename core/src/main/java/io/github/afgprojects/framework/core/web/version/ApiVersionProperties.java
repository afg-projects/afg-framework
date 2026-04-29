package io.github.afgprojects.framework.core.web.version;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * API 版本管理配置属性
 *
 * <p>配置示例:
 * <pre>{@code
 * afg:
 *   api-version:
 *     enabled: true
 *     default-version: "1.0.0"
 *     header-name: "X-API-Version"
 *     parameter-name: "version"
 *     url-prefix: "/v"
 *     deprecation:
 *       warning-header: "X-API-Deprecated"
 *       enabled: true
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "afg.api-version")
public class ApiVersionProperties {

    /**
     * 是否启用 API 版本管理
     */
    private boolean enabled = true;

    /**
     * 默认 API 版本
     * 当请求未指定版本时使用
     */
    private String defaultVersion = "1.0.0";

    /**
     * 版本请求头名称
     */
    private String headerName = "X-API-Version";

    /**
     * 版本参数名称
     */
    private String parameterName = "version";

    /**
     * URL 版本前缀
     * 例如: "/v" 匹配 /v1/resource
     */
    private String urlPrefix = "/v";

    /**
     * 版本解析策略优先级（从高到低）
     * 支持的策略: HEADER, URL, PARAMETER
     */
    private String[] resolutionOrder = {"HEADER", "URL", "PARAMETER"};

    /**
     * 废弃版本配置
     */
    private final Deprecation deprecation = new Deprecation();

    /**
     * 废弃版本配置
     */
    @Data
    public static class Deprecation {

        /**
         * 是否启用废弃版本检查
         */
        private boolean enabled = true;

        /**
         * 废弃警告响应头名称
         */
        private String warningHeader = "X-API-Deprecated";

        /**
         * 是否在日志中记录废弃版本调用
         */
        private boolean logDeprecation = true;
    }
}