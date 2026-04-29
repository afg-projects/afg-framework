package io.github.afgprojects.framework.core.datasource.lb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * 权重策略
 *
 * <p>根据配置的权重选择数据源，权重越高被选中概率越大
 *
 * @since 1.0.0
 */
public class WeightedStrategy implements LoadBalanceStrategy {

    private final Map<String, Integer> weights;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile int totalWeight;

    /**
     * 创建权重策略
     *
     * @param weights 数据源权重映射，key为数据源名称，value为权重值
     */
    public WeightedStrategy(@Nullable Map<String, Integer> weights) {
        this.weights = weights != null ? new HashMap<>(weights) : new HashMap<>();
        this.totalWeight = this.weights.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    @NonNull
    public String select(@NonNull List<String> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidates list cannot be empty");
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        // 如果没有配置权重，退化为轮询
        if (weights.isEmpty() || totalWeight <= 0) {
            return selectByRoundRobin(candidates);
        }

        // 过滤出有权重的候选数据源
        List<String> weightedCandidates = candidates.stream()
                .filter(weights::containsKey)
                .toList();

        if (weightedCandidates.isEmpty()) {
            return selectByRoundRobin(candidates);
        }

        // 计算有效总权重
        int effectiveTotalWeight = weightedCandidates.stream()
                .mapToInt(ds -> weights.getOrDefault(ds, 1))
                .sum();

        // 基于权重随机选择
        int random = ThreadLocalRandom.current().nextInt(effectiveTotalWeight);
        int currentWeight = 0;
        for (String candidate : weightedCandidates) {
            currentWeight += weights.getOrDefault(candidate, 1);
            if (random < currentWeight) {
                return candidate;
            }
        }

        return weightedCandidates.getFirst();
    }

    @NonNull
    private String selectByRoundRobin(@NonNull List<String> candidates) {
        int index = Math.abs(counter.getAndIncrement() % candidates.size());
        return candidates.get(index);
    }

    @Override
    @NonNull
    public String getName() {
        return "WEIGHTED";
    }

    /**
     * 设置数据源权重
     *
     * @param dataSource 数据源名称
     * @param weight 权重值
     */
    public void setWeight(@NonNull String dataSource, int weight) {
        int oldWeight = weights.getOrDefault(dataSource, 0);
        weights.put(dataSource, weight);
        totalWeight = totalWeight - oldWeight + weight;
    }

    /**
     * 移除数据源权重
     *
     * @param dataSource 数据源名称
     */
    public void removeWeight(@NonNull String dataSource) {
        Integer removed = weights.remove(dataSource);
        if (removed != null) {
            totalWeight -= removed;
        }
    }

    /**
     * 获取数据源权重
     *
     * @param dataSource 数据源名称
     * @return 权重值，如果未配置则返回1
     */
    public int getWeight(@NonNull String dataSource) {
        return weights.getOrDefault(dataSource, 1);
    }
}
