package io.github.afgprojects.framework.core.web.version;

import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.servlet.mvc.condition.RequestCondition;

import jakarta.servlet.http.HttpServletRequest;

/**
 * API 版本请求条件
 * 实现 Spring MVC 的 RequestCondition 接口，用于版本路由匹配
 *
 * <p>匹配规则:
 * <ul>
 *   <li>精确匹配：请求版本与 API 版本完全匹配</li>
 *   <li>主版本匹配：请求版本的主版本号与 API 版本相同</li>
 *   <li>版本降级：请求版本高于 API 版本时，匹配最近的有效版本</li>
 * </ul>
 *
 * <p>性能优化：版本解析逻辑直接在此类中执行，不依赖拦截器，
 * 只有带有 @ApiVersion 注解的请求才会触发版本匹配逻辑。
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    /**
     * 请求属性 key：解析的版本信息
     */
    public static final String RESOLVED_VERSION_ATTRIBUTE = ApiVersionRequestCondition.class.getName() + ".RESOLVED_VERSION";

    /**
     * 请求属性 key：API 版本注解信息
     */
    public static final String API_VERSION_INFO_ATTRIBUTE = ApiVersionRequestCondition.class.getName() + ".API_VERSION_INFO";

    private final ApiVersionInfo versionInfo;
    private final ApiVersionProperties properties;
    private final ApiVersionResolver versionResolver;

    /**
     * 创建版本请求条件
     *
     * @param versionInfo 版本信息
     * @param properties  版本配置
     */
    public ApiVersionRequestCondition(@NonNull ApiVersionInfo versionInfo, @NonNull ApiVersionProperties properties) {
        this.versionInfo = versionInfo;
        this.properties = properties;
        this.versionResolver = new ApiVersionResolver(properties);
    }

    /**
     * 获取版本信息
     *
     * @return 版本信息
     */
    @NonNull public ApiVersionInfo getVersionInfo() {
        return versionInfo;
    }

    @Override
    @NonNull public ApiVersionRequestCondition combine(@NonNull ApiVersionRequestCondition other) {
        // 方法级注解优先于类级注解
        return other;
    }

    @Override
    @Nullable public ApiVersionRequestCondition getMatchingCondition(@NonNull HttpServletRequest request) {
        // 直接从请求解析版本（不依赖拦截器）
        ApiVersionResolver.ResolvedVersion resolvedVersion = versionResolver.resolve(request);

        // 检查版本兼容性
        if (matches(resolvedVersion)) {
            // 存储版本信息供后续使用（如废弃警告、响应头设置）
            request.setAttribute(RESOLVED_VERSION_ATTRIBUTE, resolvedVersion);
            request.setAttribute(API_VERSION_INFO_ATTRIBUTE, versionInfo);
            return this;
        }

        return null;
    }

    /**
     * 检查是否匹配
     *
     * @param resolvedVersion 解析的版本
     * @return 是否匹配
     */
    private boolean matches(ApiVersionResolver.@NonNull ResolvedVersion resolvedVersion) {
        int requestMajor = resolvedVersion.getMajor();
        int apiMajor = versionInfo.major();

        // 主版本号相同则匹配
        return requestMajor == apiMajor;
    }

    @Override
    public int compareTo(@NonNull ApiVersionRequestCondition other, @NonNull HttpServletRequest request) {
        // 优先匹配更高版本
        return this.versionInfo.compareTo(other.versionInfo);
    }

    /**
     * 从请求中获取解析的版本信息
     *
     * @param request HTTP 请求
     * @return 解析的版本信息，如果不存在则返回 null
     */
    public static ApiVersionResolver.@Nullable ResolvedVersion getResolvedVersion(@NonNull HttpServletRequest request) {
        Object attr = request.getAttribute(RESOLVED_VERSION_ATTRIBUTE);
        if (attr instanceof ApiVersionResolver.ResolvedVersion rv) {
            return rv;
        }
        return null;
    }

    /**
     * 从请求中获取 API 版本信息
     *
     * @param request HTTP 请求
     * @return API 版本信息，如果不存在则返回 null
     */
    @Nullable
    public static ApiVersionInfo getApiVersionInfo(@NonNull HttpServletRequest request) {
        Object attr = request.getAttribute(API_VERSION_INFO_ATTRIBUTE);
        if (attr instanceof ApiVersionInfo info) {
            return info;
        }
        return null;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiVersionRequestCondition that = (ApiVersionRequestCondition) o;
        return Objects.equals(versionInfo, that.versionInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(versionInfo);
    }

    @Override
    @NonNull public String toString() {
        return "ApiVersionRequestCondition{" + versionInfo.value() + '}';
    }
}
