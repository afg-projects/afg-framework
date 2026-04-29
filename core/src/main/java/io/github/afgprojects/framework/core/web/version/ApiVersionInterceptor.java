package io.github.afgprojects.framework.core.web.version;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.afgprojects.framework.core.web.context.AfgRequestContextHolder;
import io.github.afgprojects.framework.core.web.context.RequestContext;

/**
 * API 版本拦截器
 * 处理版本请求并执行版本兼容性检查
 *
 * <p>功能:
 * <ul>
 *   <li>解析请求中的 API 版本</li>
 *   <li>检查版本兼容性</li>
 *   <li>处理废弃版本警告</li>
 *   <li>设置版本上下文信息</li>
 * </ul>
 */
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionInterceptor.class);

    /**
     * 请求属性 key：版本信息
     */
    public static final String VERSION_ATTRIBUTE = ApiVersionInterceptor.class.getName() + ".VERSION";

    /**
     * 请求属性 key：API 版本注解信息
     */
    public static final String API_VERSION_ATTRIBUTE = ApiVersionInterceptor.class.getName() + ".API_VERSION";

    /**
     * 响应头：当前 API 版本
     */
    private static final String API_VERSION_HEADER = "X-API-Version";

    private final ApiVersionResolver versionResolver;
    private final ApiVersionProperties properties;

    /**
     * 创建版本拦截器
     *
     * @param versionResolver 版本解析器
     * @param properties      版本配置
     */
    public ApiVersionInterceptor(
            @NonNull ApiVersionResolver versionResolver, @NonNull ApiVersionProperties properties) {
        this.versionResolver = versionResolver;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler)
            throws Exception {

        // 仅处理 Controller 方法
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 解析请求版本
        ApiVersionResolver.ResolvedVersion resolvedVersion = versionResolver.resolve(request);

        // 获取 API 版本注解信息
        ApiVersionInfo apiVersionInfo = extractApiVersionInfo(handlerMethod);

        // 存储版本信息到请求属性
        request.setAttribute(VERSION_ATTRIBUTE, resolvedVersion);
        if (apiVersionInfo != null) {
            request.setAttribute(API_VERSION_ATTRIBUTE, apiVersionInfo);
        }

        // 更新 RequestContext
        updateRequestContext(resolvedVersion, apiVersionInfo);

        // 执行版本兼容性检查
        if (!checkVersionCompatibility(resolvedVersion, apiVersionInfo, response)) {
            return false;
        }

        // 处理废弃版本
        handleDeprecation(apiVersionInfo, response);

        // 设置响应头
        if (apiVersionInfo != null) {
            response.setHeader(API_VERSION_HEADER, apiVersionInfo.value());
        }

        return true;
    }

    /**
     * 从 HandlerMethod 获取 @ApiVersion 注解信息
     * 方法级注解优先于类级注解
     *
     * @param handlerMethod 处理方法
     * @return API 版本信息，如果未标注则返回 null
     */
    private @Nullable ApiVersionInfo extractApiVersionInfo(@NonNull HandlerMethod handlerMethod) {
        // 优先获取方法级注解
        ApiVersion methodAnnotation = handlerMethod.getMethodAnnotation(ApiVersion.class);
        if (methodAnnotation != null) {
            return ApiVersionInfo.from(methodAnnotation);
        }

        // 获取类级注解
        ApiVersion classAnnotation = handlerMethod.getBeanType().getAnnotation(ApiVersion.class);
        if (classAnnotation != null) {
            return ApiVersionInfo.from(classAnnotation);
        }

        return null;
    }

    /**
     * 更新 RequestContext 中的版本信息
     *
     * @param resolvedVersion 解析的版本
     * @param apiVersionInfo  API 版本信息
     */
    private void updateRequestContext(
            ApiVersionResolver.ResolvedVersion resolvedVersion, @Nullable ApiVersionInfo apiVersionInfo) {
        RequestContext context = AfgRequestContextHolder.getContext();
        if (context != null) {
            context.setAttribute("apiVersion", resolvedVersion.getVersion());
            if (apiVersionInfo != null) {
                context.setAttribute("apiDeprecated", apiVersionInfo.deprecated());
            }
        }
    }

    /**
     * 检查版本兼容性
     *
     * @param resolvedVersion 请求版本
     * @param apiVersionInfo  API 版本信息
     * @param response        HTTP 响应
     * @return 是否兼容
     */
    private boolean checkVersionCompatibility(
            ApiVersionResolver.ResolvedVersion resolvedVersion,
            @Nullable ApiVersionInfo apiVersionInfo,
            @NonNull HttpServletResponse response) {

        if (apiVersionInfo == null) {
            return true;
        }

        int requestMajor = resolvedVersion.getMajor();

        // 检查版本是否在支持范围内
        if (!apiVersionInfo.isInRange(requestMajor)) {
            log.warn(
                    "API version {} is not in supported range for API {}",
                    resolvedVersion.getVersion(),
                    apiVersionInfo.value());

            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }

        // 检查主版本兼容性
        if (!apiVersionInfo.isCompatibleWith(requestMajor)) {
            log.warn(
                    "API version {} is not compatible with request version {}",
                    apiVersionInfo.value(),
                    resolvedVersion.getVersion());

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return false;
        }

        return true;
    }

    /**
     * 处理废弃版本
     *
     * @param apiVersionInfo API 版本信息
     * @param response       HTTP 响应
     */
    private void handleDeprecation(@Nullable ApiVersionInfo apiVersionInfo, @NonNull HttpServletResponse response) {
        if (apiVersionInfo == null || !apiVersionInfo.deprecated()) {
            return;
        }

        if (!properties.getDeprecation().isEnabled()) {
            return;
        }

        // 添加废弃警告头
        String warning = apiVersionInfo.buildDeprecationWarning();
        if (warning != null) {
            response.setHeader(properties.getDeprecation().getWarningHeader(), warning);

            // 记录废弃版本调用日志
            if (properties.getDeprecation().isLogDeprecation()) {
                log.warn("Deprecated API called: {}", warning);
            }
        }
    }

    /**
     * 从请求中获取解析的版本信息
     *
     * @param request HTTP 请求
     * @return 解析的版本信息，如果不存在则返回 null
     */
    @Nullable
    public static Object getResolvedVersion(@NonNull HttpServletRequest request) {
        return request.getAttribute(VERSION_ATTRIBUTE);
    }

    /**
     * 从请求中获取 API 版本信息
     *
     * @param request HTTP 请求
     * @return API 版本信息，如果不存在则返回 null
     */
    @Nullable
    public static ApiVersionInfo getApiVersionInfo(@NonNull HttpServletRequest request) {
        return (ApiVersionInfo) request.getAttribute(API_VERSION_ATTRIBUTE);
    }
}