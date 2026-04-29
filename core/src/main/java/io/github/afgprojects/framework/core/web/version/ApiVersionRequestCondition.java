package io.github.afgprojects.framework.core.web.version;

import java.util.Collection;
import java.util.Collections;
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
 */
public class ApiVersionRequestCondition implements RequestCondition<ApiVersionRequestCondition> {

    private final ApiVersionInfo versionInfo;
    private final ApiVersionProperties properties;

    /**
     * 创建版本请求条件
     *
     * @param versionInfo 版本信息
     * @param properties  版本配置
     */
    public ApiVersionRequestCondition(@NonNull ApiVersionInfo versionInfo, @NonNull ApiVersionProperties properties) {
        this.versionInfo = versionInfo;
        this.properties = properties;
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
        // 解析请求版本
        Object versionObj = ApiVersionInterceptor.getResolvedVersion(request);
        ApiVersionResolver.ResolvedVersion resolvedVersion = null;

        if (versionObj instanceof ApiVersionResolver.ResolvedVersion rv) {
            resolvedVersion = rv;
        }

        if (resolvedVersion == null) {
            // 如果没有版本信息，使用默认版本
            resolvedVersion = new ApiVersionResolver.ResolvedVersion(
                    ApiVersionInfo.of(properties.getDefaultVersion()), null, "default");
        }

        // 检查版本兼容性
        if (matches(resolvedVersion)) {
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
    private boolean matches(ApiVersionResolver.ResolvedVersion resolvedVersion) {
        int requestMajor = resolvedVersion.getMajor();
        int apiMajor = versionInfo.major();

        // 主版本号相同则匹配
        return requestMajor == apiMajor;
    }

    @Override
    public int compareTo(@NonNull ApiVersionRequestCondition other, @NonNull HttpServletRequest request) {
        // 优先匹配更高版本（compareTo 返回正数表示 this > other）
        // 我们希望更高版本优先，所以返回 this - other
        return this.versionInfo.compareTo(other.versionInfo);
    }

    // getContent 和 getToStringInfix 在 Spring 6.2+ 中为 final 方法
    // 重写 toString 以提供自定义表示

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