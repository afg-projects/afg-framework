package io.github.afgprojects.framework.ai.agent.tool;

import io.github.afgprojects.framework.ai.core.api.tool.Tool;
import io.github.afgprojects.framework.ai.core.api.tool.ToolRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 ToolRegistry 实现
 *
 * <p>线程安全的工具注册表，支持动态注册和注销工具。
 *
 * @author afg-projects
 * @since 1.0.0
 */
public class DefaultToolRegistry implements ToolRegistry {

    private final Map<String, Tool<?, ?>> tools = new ConcurrentHashMap<>();

    /**
     * 创建空注册表
     */
    public DefaultToolRegistry() {
    }

    /**
     * 创建带初始工具的注册表
     *
     * @param tools 初始工具映射
     */
    public DefaultToolRegistry(@NonNull Map<String, Tool<?, ?>> tools) {
        this.tools.putAll(tools);
    }

    @Override
    public <I, O> void register(@NonNull Tool<I, O> tool) {
        tools.put(tool.name(), tool);
    }

    @Override
    public <I, O> void registerOrReplace(@NonNull Tool<I, O> tool) {
        tools.put(tool.name(), tool);
    }

    @Override
    public @NonNull Optional<Tool<?, ?>> getTool(@NonNull String name) {
        return Optional.ofNullable(tools.get(name));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <I, O> @NonNull Optional<Tool<I, O>> getTool(@NonNull String name, @NonNull Class<I> inputType, @NonNull Class<O> outputType) {
        Tool<?, ?> tool = tools.get(name);
        if (tool == null) {
            return Optional.empty();
        }
        // Note: Type safety is not guaranteed at runtime due to type erasure
        // Caller is responsible for ensuring correct types
        return Optional.of((Tool<I, O>) tool);
    }

    @Override
    public @NonNull Collection<Tool<?, ?>> getAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    @Override
    public boolean exists(@NonNull String name) {
        return tools.containsKey(name);
    }

    @Override
    public boolean unregister(@NonNull String name) {
        return tools.remove(name) != null;
    }

    @Override
    public void clear() {
        tools.clear();
    }

    @Override
    public int size() {
        return tools.size();
    }
}
