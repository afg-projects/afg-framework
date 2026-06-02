package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 模型路由上下文 - 基于 ThreadLocal 存储当前请求的模型路由信息。
 *
 * <p>用于在请求处理链中传递模型路由信息，使下游组件能够获取当前请求的目标模型。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 设置模型路由
 * ModelRouteContext.set("gpt-4", "gpt-3.5-turbo");
 *
 * // 获取当前模型
 * String model = ModelRouteContext.getModel();
 *
 * // 清除上下文（通常在 finally 块中）
 * ModelRouteContext.clear();
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public final class ModelRouteContext {

    private static final ThreadLocal<RouteInfo> CONTEXT = new ThreadLocal<>();

    private ModelRouteContext() {
        // 私有构造函数，防止实例化
    }

    /**
     * 设置模型路由信息
     *
     * @param modelName     目标模型名称
     * @param fallbackModel 备选模型名称（可为空）
     */
    public static void set(@NonNull String modelName, @Nullable String fallbackModel) {
        CONTEXT.set(new RouteInfo(modelName, fallbackModel));
    }

    /**
     * 设置模型路由信息（无备选模型）
     *
     * @param modelName 目标模型名称
     */
    public static void set(@NonNull String modelName) {
        set(modelName, null);
    }

    /**
     * 获取当前目标模型名称
     *
     * @return 目标模型名称，如果未设置则返回 null
     */
    @Nullable
    public static String getModel() {
        RouteInfo info = CONTEXT.get();
        return info != null ? info.modelName() : null;
    }

    /**
     * 获取备选模型名称
     *
     * @return 备选模型名称，如果未设置或无备选模型则返回 null
     */
    @Nullable
    public static String getFallbackModel() {
        RouteInfo info = CONTEXT.get();
        return info != null ? info.fallbackModel() : null;
    }

    /**
     * 获取完整的路由信息
     *
     * @return 路由信息，如果未设置则返回 null
     */
    @Nullable
    public static RouteInfo getRouteInfo() {
        return CONTEXT.get();
    }

    /**
     * 判断是否已设置模型路由
     *
     * @return 是否已设置
     */
    public static boolean isSet() {
        return CONTEXT.get() != null;
    }

    /**
     * 清除当前线程的模型路由信息
     *
     * <p>应在请求处理完成后调用，通常在 finally 块中。
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 路由信息记录
     *
     * @param modelName     目标模型名称
     * @param fallbackModel 备选模型名称
     */
    public record RouteInfo(
            @NonNull String modelName,
            @Nullable String fallbackModel
    ) {
        /**
         * 判断是否有备选模型
         *
         * @return 是否有备选模型
         */
        public boolean hasFallback() {
            return fallbackModel != null && !fallbackModel.isEmpty();
        }
    }
}
