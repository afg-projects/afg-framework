package io.github.afgprojects.framework.core.datasource.lb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 读数据源负载均衡器
 *
 * <p>管理多个从库，根据策略选择从库，支持健康检查和动态添加/移除从库
 *
 * <h3>功能特性</h3>
 * <ul>
 *   <li>支持多种负载均衡策略（轮询、权重、最少连接）</li>
 *   <li>自动健康检查，剔除不健康的从库</li>
 *   <li>支持动态添加/移除从库</li>
 *   <li>支持权重配置</li>
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.AvoidCatchingGenericException")
public class ReadDataSourceLoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(ReadDataSourceLoadBalancer.class);

    private static final long DEFAULT_HEALTH_CHECK_INTERVAL = 30000L;
    private static final String HEALTH_CHECK_SQL = "SELECT 1";

    private final List<String> readDataSources = new CopyOnWriteArrayList<>();
    private final Set<String> unhealthyDataSources = ConcurrentHashMap.newKeySet();
    private final Map<String, DataSource> dataSourceMap;
    private final AtomicReference<LoadBalanceStrategy> strategy;
    private final String primaryDataSource;
    private final ScheduledExecutorService healthCheckExecutor;
    private final long healthCheckInterval;
    private final AtomicBoolean healthCheckEnabled = new AtomicBoolean(false);

    /**
     * 创建读数据源负载均衡器
     *
     * @param dataSourceMap 数据源映射
     * @param primaryDataSource 主数据源名称
     * @param readDataSources 读数据源名称列表
     * @param strategy 负载均衡策略
     */
    public ReadDataSourceLoadBalancer(
            @NonNull Map<String, DataSource> dataSourceMap,
            @NonNull String primaryDataSource,
            @NonNull List<String> readDataSources,
            @NonNull LoadBalanceStrategy strategy) {
        this(dataSourceMap, primaryDataSource, readDataSources, strategy, DEFAULT_HEALTH_CHECK_INTERVAL);
    }

    /**
     * 创建读数据源负载均衡器
     *
     * @param dataSourceMap 数据源映射
     * @param primaryDataSource 主数据源名称
     * @param readDataSources 读数据源名称列表
     * @param strategy 负载均衡策略
     * @param healthCheckInterval 健康检查间隔（毫秒）
     */
    public ReadDataSourceLoadBalancer(
            @NonNull Map<String, DataSource> dataSourceMap,
            @NonNull String primaryDataSource,
            @NonNull List<String> readDataSources,
            @NonNull LoadBalanceStrategy strategy,
            long healthCheckInterval) {
        this.dataSourceMap = new ConcurrentHashMap<>(dataSourceMap);
        this.primaryDataSource = primaryDataSource;
        this.strategy = new AtomicReference<>(strategy);
        this.healthCheckInterval = healthCheckInterval > 0 ? healthCheckInterval : DEFAULT_HEALTH_CHECK_INTERVAL;
        this.healthCheckExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "datasource-health-check");
            t.setDaemon(true);
            return t;
        });
        this.readDataSources.addAll(readDataSources);
    }

    /**
     * 选择一个可用的读数据源
     *
     * @return 数据源名称，如果没有可用的从库则返回主数据源
     */
    @NonNull
    public String select() {
        List<String> healthyCandidates = getHealthyCandidates();
        if (healthyCandidates.isEmpty()) {
            log.warn("No healthy read data source available, falling back to primary: {}", primaryDataSource);
            return primaryDataSource;
        }
        String selected = strategy.get().select(healthyCandidates);

        // 如果是最少连接策略，增加连接计数
        if (strategy.get() instanceof LeastConnectionsStrategy leastConnStrategy) {
            leastConnStrategy.incrementConnection(selected);
        }

        return selected;
    }

    /**
     * 释放数据源连接
     *
     * <p>对于最少连接策略，需要调用此方法减少连接计数
     *
     * @param dataSource 数据源名称
     */
    public void release(@NonNull String dataSource) {
        if (strategy.get() instanceof LeastConnectionsStrategy leastConnStrategy) {
            leastConnStrategy.decrementConnection(dataSource);
        }
    }

    /**
     * 获取健康的候选数据源
     *
     * @return 健康的数据源列表
     */
    @NonNull
    public List<String> getHealthyCandidates() {
        return readDataSources.stream()
                .filter(ds -> !unhealthyDataSources.contains(ds))
                .toList();
    }

    /**
     * 添加读数据源
     *
     * @param dataSourceName 数据源名称
     * @param dataSource 数据源实例
     */
    public void addReadDataSource(@NonNull String dataSourceName, @NonNull DataSource dataSource) {
        dataSourceMap.put(dataSourceName, dataSource);
        if (!readDataSources.contains(dataSourceName)) {
            readDataSources.add(dataSourceName);
        }
        unhealthyDataSources.remove(dataSourceName);
        log.info("Added read data source: {}", dataSourceName);
    }

    /**
     * 移除读数据源
     *
     * @param dataSourceName 数据源名称
     */
    public void removeReadDataSource(@NonNull String dataSourceName) {
        readDataSources.remove(dataSourceName);
        dataSourceMap.remove(dataSourceName);
        unhealthyDataSources.remove(dataSourceName);

        if (strategy.get() instanceof LeastConnectionsStrategy leastConnStrategy) {
            leastConnStrategy.removeDataSource(dataSourceName);
        }
        if (strategy.get() instanceof WeightedStrategy weightedStrategy) {
            weightedStrategy.removeWeight(dataSourceName);
        }

        log.info("Removed read data source: {}", dataSourceName);
    }

    /**
     * 标记数据源为不健康
     *
     * @param dataSourceName 数据源名称
     */
    public void markUnhealthy(@NonNull String dataSourceName) {
        if (readDataSources.contains(dataSourceName)) {
            unhealthyDataSources.add(dataSourceName);
            log.warn("Marked data source as unhealthy: {}", dataSourceName);
        }
    }

    /**
     * 标记数据源为健康
     *
     * @param dataSourceName 数据源名称
     */
    public void markHealthy(@NonNull String dataSourceName) {
        unhealthyDataSources.remove(dataSourceName);
        log.info("Marked data source as healthy: {}", dataSourceName);
    }

    /**
     * 设置权重
     *
     * <p>仅对权重策略有效
     *
     * @param dataSourceName 数据源名称
     * @param weight 权重值
     */
    public void setWeight(@NonNull String dataSourceName, int weight) {
        if (strategy.get() instanceof WeightedStrategy weightedStrategy) {
            weightedStrategy.setWeight(dataSourceName, weight);
            log.debug("Set weight for data source {}: {}", dataSourceName, weight);
        }
    }

    /**
     * 设置负载均衡策略
     *
     * @param newStrategy 新策略
     */
    public void setStrategy(@NonNull LoadBalanceStrategy newStrategy) {
        LoadBalanceStrategy oldStrategy = strategy.getAndSet(newStrategy);
        log.info("Load balance strategy changed from {} to {}", oldStrategy.getName(), newStrategy.getName());
    }

    /**
     * 获取当前策略
     *
     * @return 负载均衡策略
     */
    @NonNull
    public LoadBalanceStrategy getStrategy() {
        return strategy.get();
    }

    /**
     * 启动健康检查
     */
    public void startHealthCheck() {
        if (healthCheckEnabled.compareAndSet(false, true)) {
            healthCheckExecutor.scheduleAtFixedRate(
                    this::performHealthCheck,
                    healthCheckInterval,
                    healthCheckInterval,
                    TimeUnit.MILLISECONDS);
            log.info("Health check started with interval: {}ms", healthCheckInterval);
        }
    }

    /**
     * 停止健康检查
     */
    public void stopHealthCheck() {
        if (healthCheckEnabled.compareAndSet(true, false)) {
            healthCheckExecutor.shutdown();
            try {
                if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    healthCheckExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            log.info("Health check stopped");
        }
    }

    /**
     * 执行健康检查
     */
    private void performHealthCheck() {
        for (String dataSourceName : readDataSources) {
            DataSource dataSource = dataSourceMap.get(dataSourceName);
            if (dataSource == null) {
                markUnhealthy(dataSourceName);
                continue;
            }

            try (var conn = dataSource.getConnection();
                 var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(HEALTH_CHECK_SQL)) {
                if (rs.next()) {
                    markHealthy(dataSourceName);
                }
            } catch (Exception e) {
                log.warn("Health check failed for data source {}: {}", dataSourceName, e.getMessage());
                markUnhealthy(dataSourceName);
            }
        }
    }

    /**
     * 获取所有读数据源
     *
     * @return 读数据源名称列表
     */
    @NonNull
    public List<String> getReadDataSources() {
        return List.copyOf(readDataSources);
    }

    /**
     * 获取不健康的数据源
     *
     * @return 不健康数据源名称集合
     */
    @NonNull
    public Set<String> getUnhealthyDataSources() {
        return Set.copyOf(unhealthyDataSources);
    }

    /**
     * 获取主数据源名称
     *
     * @return 主数据源名称
     */
    @NonNull
    public String getPrimaryDataSource() {
        return primaryDataSource;
    }

    /**
     * 是否启用健康检查
     *
     * @return true表示健康检查正在运行
     */
    public boolean isHealthCheckEnabled() {
        return healthCheckEnabled.get();
    }

    /**
     * 手动触发健康检查
     */
    public void triggerHealthCheck() {
        performHealthCheck();
    }
}