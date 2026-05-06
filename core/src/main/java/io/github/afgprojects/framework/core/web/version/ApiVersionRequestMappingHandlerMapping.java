package io.github.afgprojects.framework.core.web.version;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
 *   <li>支持 URL 路径前缀版本（如 /v1/users）</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>{@code
 * // 方式一：使用 @ApiVersion 注解
 * @RestController
 * @ApiVersion("1.0")
 * @RequestMapping("/users")
 * public class UserApiV1 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 *
 * // 方式二：使用 URL 路径前缀
 * @RestController
 * @ApiVersion("2.0")
 * @RequestMapping("/v2/users")
 * public class UserApiV2 {
 *     @GetMapping
 *     public List<User> getUsers() { ... }
 * }
 *
 * // 方式三：方法级版本标记
 * @RestController
 * @RequestMapping("/users")
 * public class UserApi {
 *     @ApiVersion("1.0")
 *     @GetMapping
 *     public List<User> getUsersV1() { ... }
 *
 *     @ApiVersion("2.0")
 *     @GetMapping
 *     public List<User> getUsersV2() { ... }
 * }
 * }</pre>
 *
 * <p>路由规则:
 * <ul>
 *   <li>请求版本 1.x -> UserApiV1</li>
 *   <li>请求版本 2.x -> UserApiV2</li>
 *   <li>无版本请求 -> 根据 defaultVersion 配置决定</li>
 *   <li>URL 路径前缀版本 -> 匹配对应的 @ApiVersion</li>
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
     * URL 版本前缀匹配模式
     * 匹配 /v{major} 或 /v{major}.{minor}
     */
    private final Pattern urlVersionPattern;

    /**
     * 创建版本路由映射处理器
     *
     * @param properties 版本配置
     */
    public ApiVersionRequestMappingHandlerMapping(@NonNull ApiVersionProperties properties) {
        this.properties = properties;
        // 构建 URL 版本匹配正则
        String urlPrefix = Pattern.quote(properties.getUrlPrefix());
        this.urlVersionPattern = Pattern.compile(urlPrefix + "(\\d+)(?:\\.(\\d+))?");
    }

    @Override
    protected RequestCondition<?> getCustomTypeCondition(@NonNull Class<?> handlerType) {
        return createVersionCondition(handlerType);
    }

    @Override
    protected RequestCondition<?> getCustomMethodCondition(@NonNull Method method) {
        return createVersionCondition(method);
    }

    /**
     * 重写此方法，只处理带有 @ApiVersion 注解的 Controller
     * 如果 Controller 没有 @ApiVersion 注解，返回 null，不注册映射
     *
     * <p>这样可以确保：
     * <ul>
     *   <li>带有 @ApiVersion 注解的 Controller 由本 HandlerMapping 处理</li>
     *   <li>不带 @ApiVersion 注解的 Controller 由默认的 RequestMappingHandlerMapping 处理</li>
     * </ul>
     */
    @Override
    protected RequestMappingInfo getMappingForMethod(@NonNull Method method, @NonNull Class<?> handlerType) {
        // 检查类或方法上是否有 @ApiVersion 注解
        ApiVersion classAnnotation = findApiVersionAnnotation(handlerType);
        ApiVersion methodAnnotation = findApiVersionAnnotation(method);

        // 如果没有 @ApiVersion 注解，不注册映射（交给默认的 HandlerMapping 处理）
        if (classAnnotation == null && methodAnnotation == null) {
            log.debug("No @ApiVersion annotation found on {}.{}, skipping version mapping",
                    handlerType.getSimpleName(), method.getName());
            return null;
        }

        // 有 @ApiVersion 注解，使用父类方法创建映射
        RequestMappingInfo mappingInfo = super.getMappingForMethod(method, handlerType);

        if (mappingInfo != null) {
            ApiVersionInfo versionInfo = methodAnnotation != null
                    ? ApiVersionInfo.from(methodAnnotation)
                    : ApiVersionInfo.from(classAnnotation);

            log.debug("Created version mapping for {}.{}, version: {}, patterns: {}",
                    handlerType.getSimpleName(),
                    method.getName(),
                    versionInfo.value(),
                    mappingInfo.getPatternValues());
        }

        return mappingInfo;
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
                String normalizedPattern = normalizePattern(pattern);
                versionMappings
                        .computeIfAbsent(normalizedPattern, k -> new ArrayList<>())
                        .add(new VersionMapping(versionInfo, mappingInfo, handler));

                log.debug("Version mapping: {} -> version {}, pattern: {}",
                        pattern, versionInfo.value(), normalizedPattern);
            }
        }

        // 按版本号排序
        for (List<VersionMapping> mappings : versionMappings.values()) {
            mappings.sort(Comparator.comparing(VersionMapping::versionInfo, ApiVersionInfo::compareTo));
        }
    }

    /**
     * 从处理器中提取版本信息
     *
     * @param handler 处理器
     * @return 版本信息，如果不存在则返回 null
     */
    private @Nullable ApiVersionInfo extractVersionInfo(@NonNull Object handler) {
        if (handler instanceof HandlerMethod handlerMethod) {
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
        }
        return null;
    }

    /**
     * 标准化路径模式
     * 移除 URL 版本前缀，以便统一匹配
     *
     * @param pattern 路径模式
     * @return 标准化后的模式
     */
    private @NonNull String normalizePattern(@NonNull String pattern) {
        // 移除版本前缀（如 /v1/users -> /users）
        String urlPrefix = properties.getUrlPrefix();
        if (pattern.contains(urlPrefix)) {
            String normalized = pattern.replaceFirst(urlVersionPattern.pattern(), "");
            log.debug("Normalized pattern: {} -> {}", pattern, normalized);
            return normalized;
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
