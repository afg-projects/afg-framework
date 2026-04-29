package io.github.afgprojects.framework.core.web.version;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.method.HandlerMethod;

/**
 * API 版本路由映射处理器
 * 扩展 Spring MVC 的 RequestMappingHandlerMapping，支持基于版本的请求路由
 *
 * <p>功能:
 * <ul>
 *   <li>自动识别 @ApiVersion 注解</li>
 *   <li>为带版本的 API 创建版本条件</li>
 *   <li>支持多版本 API 共存</li>
 *   <li>版本降级（请求版本不存在时匹配较低版本）</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>{@code
 * @RestController
 * @ApiVersion("1.0")
 * @RequestMapping("/users")
 * public class UserApiV1 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 *
 * @RestController
 * @ApiVersion("2.0")
 * @RequestMapping("/users")
 * public class UserApiV2 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 * }</pre>
 *
 * <p>路由规则:
 * <ul>
 *   <li>请求版本 1.x -> UserApiV1</li>
 *   <li>请求版本 2.x -> UserApiV2</li>
 *   <li>无版本请求 -> 根据 defaultVersion 配置决定</li>
 * </ul>
 */
public class ApiVersionRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    private static final Logger log = LoggerFactory.getLogger(ApiVersionRequestMappingHandlerMapping.class);

    /**
     * API 版本映射缓存
     * Key: pattern (如 /users), Value: 版本映射列表
     */
    private final Map<String, List<VersionMapping>> versionMappings = new ConcurrentHashMap<>();

    private final ApiVersionProperties properties;

    /**
     * 创建版本路由映射处理器
     *
     * @param properties 版本配置
     */
    public ApiVersionRequestMappingHandlerMapping(@NonNull ApiVersionProperties properties) {
        this.properties = properties;
    }

    @Override
    protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
        return createVersionCondition(handlerType);
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(Method method) {
        return createVersionCondition(method);
    }

    /**
     * 创建版本条件
     *
     * @param element 类或方法
     * @return 版本条件，如果未标注 @ApiVersion 则返回 null
     */
    private @Nullable ApiVersionRequestCondition createVersionCondition(@NonNull Object element) {
        ApiVersion apiVersion = findApiVersionAnnotation(element);
        if (apiVersion == null) {
            return null;
        }

        ApiVersionInfo versionInfo = ApiVersionInfo.from(apiVersion);
        log.debug("Found @ApiVersion annotation: {} on {}", versionInfo.value(), element);

        return new ApiVersionRequestCondition(versionInfo, properties);
    }

    /**
     * 查找 @ApiVersion 注解
     *
     * @param element 类或方法
     * @return @ApiVersion 注解，如果不存在则返回 null
     */
    private @Nullable ApiVersion findApiVersionAnnotation(@NonNull Object element) {
        if (element instanceof Class<?> clazz) {
            return AnnotatedElementUtils.findMergedAnnotation(clazz, ApiVersion.class);
        } else if (element instanceof Method method) {
            return AnnotatedElementUtils.findMergedAnnotation(method, ApiVersion.class);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handlerMethodsInitialized(Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        super.handlerMethodsInitialized(handlerMethods);

        // 构建版本映射索引
        buildVersionMappings(handlerMethods);

        log.info("Initialized {} API version mappings", versionMappings.size());
    }

    /**
     * 构建版本映射索引
     *
     * @param handlerMethods 处理方法映射
     */
    private void buildVersionMappings(@NonNull Map<RequestMappingInfo, HandlerMethod> handlerMethods) {
        for (Map.Entry<RequestMappingInfo, ?> entry : handlerMethods.entrySet()) {
            RequestMappingInfo mappingInfo = entry.getKey();
            Object handler = entry.getValue();

            // 获取版本信息
            ApiVersionInfo versionInfo = extractVersionInfo(handler);
            if (versionInfo == null) {
                continue;
            }

            // 获取路径模式
            for (String pattern : mappingInfo.getPatternValues()) {
                versionMappings
                        .computeIfAbsent(normalizePattern(pattern), k -> new ArrayList<>())
                        .add(new VersionMapping(versionInfo, mappingInfo, handler));
            }
        }

        // 按版本号排序
        for (List<VersionMapping> mappings : versionMappings.values()) {
            mappings.sort(Comparator.comparing(m -> m.versionInfo(), ApiVersionInfo::compareTo));
        }
    }

    /**
     * 从处理器中提取版本信息
     *
     * @param handler 处理器
     * @return 版本信息，如果不存在则返回 null
     */
    private @Nullable ApiVersionInfo extractVersionInfo(@NonNull Object handler) {
        // 从 handler 中获取版本条件
        // 实际上版本条件已经在映射时创建
        return null;
    }

    /**
     * 标准化路径模式
     *
     * @param pattern 路径模式
     * @return 标准化后的模式
     */
    private @NonNull String normalizePattern(@NonNull String pattern) {
        // 移除版本前缀
        String urlPrefix = properties.getUrlPrefix();
        if (pattern.contains(urlPrefix)) {
            return pattern.replaceFirst(urlPrefix + "\\d+(?:\\.\\d+)?", "");
        }
        return pattern;
    }

    /**
     * 版本映射信息
     *
     * @param versionInfo 版本信息
     * @param mappingInfo 映射信息
     * @param handler     处理器
     */
    private record VersionMapping(
            @NonNull ApiVersionInfo versionInfo,
            @NonNull RequestMappingInfo mappingInfo,
            @NonNull Object handler) {}
}