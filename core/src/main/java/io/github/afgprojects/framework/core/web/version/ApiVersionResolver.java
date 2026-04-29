package io.github.afgprojects.framework.core.web.version;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 版本解析器
 * 从请求中解析 API 版本信息
 *
 * <p>支持的解析策略（按优先级）:
 * <ol>
 *   <li>HEADER - 从请求头获取版本（X-API-Version: 1.0）</li>
 *   <li>URL - 从 URL 路径提取版本（/v1/users）</li>
 *   <li>PARAMETER - 从请求参数获取版本（?version=1.0）</li>
 * </ol>
 */
public class ApiVersionResolver {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionResolver.class);

    /**
     * 版本解析策略
     */
    public enum Strategy {
        /**
         * 从请求头解析
         */
        HEADER,
        /**
         * 从 URL 路径解析
         */
        URL,
        /**
         * 从请求参数解析
         */
        PARAMETER
    }

    private final ApiVersionProperties properties;

    /**
     * URL 版本匹配模式
     * 匹配 /v{major} 或 /v{major}.{minor}
     */
    private final Pattern urlVersionPattern;

    /**
     * 创建版本解析器
     *
     * @param properties 版本配置
     */
    public ApiVersionResolver(@NonNull ApiVersionProperties properties) {
        this.properties = properties;
        // 构建 URL 版本匹配正则
        String urlPrefix = Pattern.quote(properties.getUrlPrefix());
        this.urlVersionPattern = Pattern.compile(urlPrefix + "(\\d+)(?:\\.(\\d+))?");
    }

    /**
     * 从请求中解析 API 版本
     *
     * @param request HTTP 请求
     * @return 解析出的版本信息，如果无法解析则返回默认版本
     */
    @NonNull public ResolvedVersion resolve(@NonNull HttpServletRequest request) {
        // 按配置的优先级顺序解析
        for (String strategyName : properties.getResolutionOrder()) {
            Strategy strategy = Strategy.valueOf(strategyName.toUpperCase());
            ResolvedVersion version = resolveByStrategy(request, strategy);
            if (version != null) {
                log.debug("Resolved API version {} using {} strategy", version, strategy);
                return version;
            }
        }

        // 返回默认版本
        return new ResolvedVersion(
                ApiVersionInfo.of(properties.getDefaultVersion()),
                null,
                "default");
    }

    /**
     * 使用指定策略解析版本
     *
     * @param request  HTTP 请求
     * @param strategy 解析策略
     * @return 解析出的版本信息，如果无法解析则返回 null
     */
    private @Nullable ResolvedVersion resolveByStrategy(@NonNull HttpServletRequest request, Strategy strategy) {
        return switch (strategy) {
            case HEADER -> resolveFromHeader(request);
            case URL -> resolveFromUrl(request);
            case PARAMETER -> resolveFromParameter(request);
        };
    }

    /**
     * 从请求头解析版本
     *
     * @param request HTTP 请求
     * @return 解析出的版本信息，如果无法解析则返回 null
     */
    private @Nullable ResolvedVersion resolveFromHeader(@NonNull HttpServletRequest request) {
        String headerValue = request.getHeader(properties.getHeaderName());
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }

        try {
            return new ResolvedVersion(
                    ApiVersionInfo.of(headerValue.trim()),
                    headerValue.trim(),
                    "header:" + properties.getHeaderName());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid API version in header: {}", headerValue);
            return null;
        }
    }

    /**
     * 从 URL 路径解析版本
     *
     * @param request HTTP 请求
     * @return 解析出的版本信息，如果无法解析则返回 null
     */
    private @Nullable ResolvedVersion resolveFromUrl(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();

        // 匹配 URL 中的版本
        Matcher matcher = urlVersionPattern.matcher(uri);
        if (!matcher.find()) {
            return null;
        }

        try {
            int major = Integer.parseInt(matcher.group(1));
            String minorStr = matcher.group(2);
            int minor = minorStr != null ? Integer.parseInt(minorStr) : 0;

            String version = major + "." + minor;
            return new ResolvedVersion(
                    new ApiVersionInfo(version, major, minor, false, null, null, null, null),
                    version,
                    "url:" + matcher.group());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse version from URL: {}", uri);
            return null;
        }
    }

    /**
     * 从请求参数解析版本
     *
     * @param request HTTP 请求
     * @return 解析出的版本信息，如果无法解析则返回 null
     */
    private @Nullable ResolvedVersion resolveFromParameter(@NonNull HttpServletRequest request) {
        String paramValue = request.getParameter(properties.getParameterName());
        if (paramValue == null || paramValue.isBlank()) {
            return null;
        }

        try {
            return new ResolvedVersion(
                    ApiVersionInfo.of(paramValue.trim()),
                    paramValue.trim(),
                    "parameter:" + properties.getParameterName());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid API version in parameter: {}", paramValue);
            return null;
        }
    }

    /**
     * 解析后的版本信息
     *
     * @param versionInfo 版本信息
     * @param rawValue    原始值
     * @param source      来源描述
     */
    public record ResolvedVersion(
            @NonNull ApiVersionInfo versionInfo,
            @Nullable String rawValue,
            @Nullable String source) {

        /**
         * 获取主版本号
         */
        public int getMajor() {
            return versionInfo.major();
        }

        /**
         * 获取次版本号
         */
        public int getMinor() {
            return versionInfo.minor();
        }

        /**
         * 获取完整版本字符串
         */
        @NonNull public String getVersion() {
            return versionInfo.value();
        }

        @Override
        @NonNull public String toString() {
            return versionInfo.value() + (source != null ? " (from " + source + ")" : "");
        }
    }
}