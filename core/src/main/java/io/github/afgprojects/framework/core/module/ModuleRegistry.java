package io.github.afgprojects.framework.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.github.afgprojects.framework.core.module.exception.ModuleCircularDependencyException;
import io.github.afgprojects.framework.core.module.exception.ModuleDuplicateException;
import io.github.afgprojects.framework.core.module.exception.ModuleNotFoundException;

/**
 * 模块注册表
 * 管理所有已注册的模块，提供依赖解析和排序功能
 */
@SuppressWarnings({"PMD.LooseCoupling"})
public class ModuleRegistry {

    private final Map<String, ModuleDefinition> modules = new ConcurrentHashMap<>();

    /**
     * 注册模块
     *
     * @param definition 模块定义
     * @throws ModuleDuplicateException 如果模块ID已存在
     * @throws ModuleNotFoundException  如果依赖的模块不存在
     */
    public void register(@NonNull ModuleDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        modules.compute(definition.id(), (key, existing) -> {
            if (existing != null) {
                throw new ModuleDuplicateException(key);
            }
            for (String dependencyId : definition.dependencies()) {
                if (!modules.containsKey(dependencyId)) {
                    throw new ModuleNotFoundException(dependencyId, definition.id());
                }
            }
            return definition;
        });
    }

    /**
     * 注册模块（跳过依赖检查）
     * 用于内部测试和特殊场景
     *
     * @param definition 模块定义
     * @throws ModuleDuplicateException 如果模块ID已存在
     */
    public void registerWithoutDependencyCheck(@NonNull ModuleDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        ModuleDefinition existing = modules.putIfAbsent(definition.id(), definition);
        if (existing != null) {
            throw new ModuleDuplicateException(definition.id());
        }
    }

    /**
     * 获取模块定义
     *
     * @param moduleId 模块ID
     * @return 模块定义，不存在返回null
     */
    public @Nullable ModuleDefinition getModule(@NonNull String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        return modules.get(moduleId);
    }

    /**
     * 获取所有已注册的模块
     *
     * @return 模块列表
     */
    @NonNull public List<ModuleDefinition> getAllModules() {
        return new ArrayList<>(modules.values());
    }

    /**
     * 检查模块是否存在
     *
     * @param moduleId 模块ID
     * @return 是否存在
     */
    public boolean containsModule(@NonNull String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        return modules.containsKey(moduleId);
    }

    /**
     * 获取拓扑排序后的模块列表
     * 依赖模块会在被依赖模块之后返回
     *
     * @return 排序后的模块列表
     * @throws ModuleCircularDependencyException 如果存在循环依赖
     */
    @NonNull public List<ModuleDefinition> getSortedModules() {
        // 使用 Kahn 算法进行拓扑排序
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> graph = new HashMap<>();

        // 初始化
        for (String moduleId : modules.keySet()) {
            inDegree.put(moduleId, 0);
            graph.put(moduleId, new ArrayList<>());
        }

        // 构建图和入度
        for (ModuleDefinition definition : modules.values()) {
            for (String depId : definition.dependencies()) {
                if (!graph.containsKey(depId)) {
                    continue;
                }
                graph.get(depId).add(definition.id());
                inDegree.merge(definition.id(), 1, Integer::sum);
            }
        }

        // 使用优先队列确保相同入度的模块按ID排序
        PriorityQueue<String> queue = new PriorityQueue<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<ModuleDefinition> result = new ArrayList<>();
        int processedCount = 0;

        while (!queue.isEmpty()) {
            String moduleId = queue.poll();
            result.add(modules.get(moduleId));
            processedCount++;

            for (String dependentId : graph.get(moduleId)) {
                int newDegree = inDegree.get(dependentId) - 1;
                inDegree.put(dependentId, newDegree);
                if (newDegree == 0) {
                    queue.offer(dependentId);
                }
            }
        }

        // 如果处理的数量不等于总模块数，说明存在循环依赖
        if (processedCount != modules.size()) {
            List<String> cycleChain = findCycleChain();
            throw new ModuleCircularDependencyException(cycleChain);
        }

        return result;
    }

    /**
     * 查找循环依赖链
     *
     * @return 循环依赖链
     */
    @NonNull private List<String> findCycleChain() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> path = new ArrayList<>();

        for (String moduleId : modules.keySet()) {
            if (detectCycle(moduleId, visited, recursionStack, path)) {
                return path;
            }
        }

        return Collections.singletonList("unknown");
    }

    /**
     * 检测循环依赖
     */
    private boolean detectCycle(
            @NonNull String moduleId,
            @NonNull Set<String> visited,
            @NonNull Set<String> recursionStack,
            @NonNull List<String> path) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        if (recursionStack.contains(moduleId)) {
            int startIndex = path.indexOf(moduleId);
            if (startIndex >= 0) {
                path.subList(0, startIndex).clear();
            }
            path.add(moduleId);
            return true;
        }

        if (visited.contains(moduleId)) {
            return false;
        }

        visited.add(moduleId);
        recursionStack.add(moduleId);
        path.add(moduleId);

        ModuleDefinition definition = modules.get(moduleId);
        if (definition != null) {
            for (String depId : definition.dependencies()) {
                if (modules.containsKey(depId) && detectCycle(depId, visited, recursionStack, path)) {
                    return true;
                }
            }
        }

        recursionStack.remove(moduleId);
        path.remove(path.size() - 1);
        return false;
    }

    /**
     * 注销模块
     *
     * @param moduleId 模块ID
     * @throws IllegalStateException 如果模块被其他模块依赖
     */
    public void unregister(@NonNull String moduleId) {
        Objects.requireNonNull(moduleId, "moduleId must not be null");
        if (!modules.containsKey(moduleId)) {
            return;
        }

        List<String> dependents = modules.values().stream()
                .filter(def -> def.dependencies().contains(moduleId))
                .map(ModuleDefinition::id)
                .collect(Collectors.toList());

        if (!dependents.isEmpty()) {
            throw new IllegalStateException(
                    "Cannot unregister module '" + moduleId + "' because it is depended on by: " + dependents);
        }

        modules.remove(moduleId);
    }

    /**
     * 清空所有模块
     */
    public void clear() {
        modules.clear();
    }

    /**
     * 获取模块数量
     *
     * @return 模块数量
     */
    public int size() {
        return modules.size();
    }
}
