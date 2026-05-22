package io.github.afgprojects.framework.ai.core.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 多模型注册表
 *
 * <p>支持注册和获取多个 LLM 客户端，每个客户端有一个名称标识。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 注册模型
 * registry.register("main", openaiClient);
 * registry.register("local", ollamaClient);
 * registry.register("fast", fastClient);
 *
 * // 获取模型
 * LlmClient main = registry.get("main");
 * LlmClient local = registry.get("local");
 *
 * // 获取默认模型
 * LlmClient defaultClient = registry.getDefault();
 * }</pre>
 *
 * @author afg-projects
 * @since 1.0.0
 */
public interface LlmClientRegistry {

    /**
     * 注册 LLM 客户端
     *
     * @param name   客户端名称
     * @param client LLM 客户端
     * @throws IllegalArgumentException 如果名称已存在
     */
    void register(@NonNull String name, @NonNull LlmClient client);

    /**
     * 注册或替换 LLM 客户端
     *
     * @param name   客户端名称
     * @param client LLM 客户端
     */
    void registerOrReplace(@NonNull String name, @NonNull LlmClient client);

    /**
     * 获取 LLM 客户端
     *
     * @param name 客户端名称
     * @return LLM 客户端，如果不存在返回空
     */
    @NonNull
    Optional<LlmClient> get(@NonNull String name);

    /**
     * 获取默认 LLM 客户端
     *
     * <p>默认客户端是名称为 "default" 的客户端，或者第一个注册的客户端。
     *
     * @return 默认 LLM 客户端
     * @throws IllegalStateException 如果没有注册任何客户端
     */
    @NonNull
    LlmClient getDefault();

    /**
     * 设置默认客户端名称
     *
     * @param name 客户端名称
     */
    void setDefault(@NonNull String name);

    /**
     * 检查客户端是否存在
     *
     * @param name 客户端名称
     * @return 是否存在
     */
    boolean exists(@NonNull String name);

    /**
     * 注销客户端
     *
     * @param name 客户端名称
     * @return 是否成功注销
     */
    boolean unregister(@NonNull String name);

    /**
     * 获取所有客户端名称
     *
     * @return 客户端名称集合
     */
    @NonNull
    Collection<String> getNames();

    /**
     * 获取所有客户端
     *
     * @return 客户端映射
     */
    @NonNull
    Map<String, LlmClient> getAll();

    /**
     * 获取客户端数量
     *
     * @return 客户端数量
     */
    int size();

    /**
     * 清空所有客户端
     */
    void clear();
}
