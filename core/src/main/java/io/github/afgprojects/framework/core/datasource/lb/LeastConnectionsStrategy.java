package io.github.afgprojects.framework.core.datasource.lb;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

/**
 * 最少连接策略
 *
 * <p>选择当前活跃连接数最少的数据源
 *
 * @since 1.0.0
 */
public class LeastConnectionsStrategy implements LoadBalanceStrategy {

    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    @Override
    @NonNull
    public String select(@NonNull List<String> candidates) {
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("Candidates list cannot be empty");
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        String selected = null;
        int minConnections = Integer.MAX_VALUE;

        for (String candidate : candidates) {
            int connections = getConnectionCount(candidate);
            if (connections < minConnections) {
                minConnections = connections;
                selected = candidate;
            }
        }

        return selected != null ? selected : candidates.getFirst();
    }

    @Override
    @NonNull
    public String getName() {
        return "LEAST_CONNECTIONS";
    }

    /**
     * 增加数据源连接计数
     *
     * <p>在获取数据源连接后调用
     *
     * @param dataSource 数据源名称
     */
    public void incrementConnection(@NonNull String dataSource) {
        connectionCounts.computeIfAbsent(dataSource, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 减少数据源连接计数
     *
     * <p>在释放数据源连接后调用
     *
     * @param dataSource 数据源名称
     */
    public void decrementConnection(@NonNull String dataSource) {
        AtomicInteger counter = connectionCounts.get(dataSource);
        if (counter != null && counter.get() > 0) {
            counter.decrementAndGet();
        }
    }

    /**
     * 获取数据源当前连接数
     *
     * @param dataSource 数据源名称
     * @return 当前连接数
     */
    public int getConnectionCount(@NonNull String dataSource) {
        AtomicInteger counter = connectionCounts.get(dataSource);
        return counter != null ? counter.get() : 0;
    }

    /**
     * 重置数据源连接计数
     *
     * @param dataSource 数据源名称
     */
    public void resetConnectionCount(@NonNull String dataSource) {
        AtomicInteger counter = connectionCounts.get(dataSource);
        if (counter != null) {
            counter.set(0);
        }
    }

    /**
     * 获取所有数据源的连接计数快照
     *
     * @return 连接计数映射
     */
    @NonNull
    public Map<String, Integer> getConnectionCounts() {
        Map<String, Integer> result = new java.util.HashMap<>();
        connectionCounts.forEach((ds, counter) -> result.put(ds, counter.get()));
        return result;
    }

    /**
     * 移除数据源
     *
     * @param dataSource 数据源名称
     */
    public void removeDataSource(@NonNull String dataSource) {
        connectionCounts.remove(dataSource);
    }
}