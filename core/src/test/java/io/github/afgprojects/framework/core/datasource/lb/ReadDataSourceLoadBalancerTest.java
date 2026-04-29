package io.github.afgprojects.framework.core.datasource.lb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * ReadDataSourceLoadBalancer 单元测试
 */
@DisplayName("ReadDataSourceLoadBalancer 测试")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReadDataSourceLoadBalancerTest {

    @Mock
    private DataSource dataSource1;

    @Mock
    private DataSource dataSource2;

    @Mock
    private DataSource dataSource3;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private ReadDataSourceLoadBalancer loadBalancer;
    private Map<String, DataSource> dataSourceMap;

    @BeforeEach
    void setUp() throws Exception {
        dataSourceMap = new HashMap<>();
        dataSourceMap.put("master", dataSource1);
        dataSourceMap.put("slave_1", dataSource2);
        dataSourceMap.put("slave_2", dataSource3);

        // 设置 mock 行为（使用 lenient 以避免 UnnecessaryStubbingException）
        lenient().when(dataSource2.getConnection()).thenReturn(connection);
        lenient().when(dataSource3.getConnection()).thenReturn(connection);
        lenient().when(connection.createStatement()).thenReturn(statement);
        lenient().when(statement.executeQuery("SELECT 1")).thenReturn(resultSet);
        lenient().when(resultSet.next()).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (loadBalancer != null) {
            loadBalancer.stopHealthCheck();
        }
    }

    @Test
    @DisplayName("使用轮询策略选择数据源")
    void select_roundRobin_shouldRotate() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        String first = loadBalancer.select();
        String second = loadBalancer.select();

        assertThat(first).isEqualTo("slave_1");
        assertThat(second).isEqualTo("slave_2");
    }

    @Test
    @DisplayName("没有健康的从库时返回主数据源")
    void select_noHealthySlaves_shouldReturnPrimary() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.markUnhealthy("slave_1");
        loadBalancer.markUnhealthy("slave_2");

        String result = loadBalancer.select();

        assertThat(result).isEqualTo("master");
    }

    @Test
    @DisplayName("添加读数据源")
    void addReadDataSource_shouldAdd() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1"),
                new RoundRobinStrategy());

        loadBalancer.addReadDataSource("slave_2", dataSource3);

        List<String> dataSources = loadBalancer.getReadDataSources();
        assertThat(dataSources).containsExactly("slave_1", "slave_2");
    }

    @Test
    @DisplayName("移除读数据源")
    void removeReadDataSource_shouldRemove() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.removeReadDataSource("slave_2");

        List<String> dataSources = loadBalancer.getReadDataSources();
        assertThat(dataSources).containsExactly("slave_1");
    }

    @Test
    @DisplayName("标记不健康的数据源")
    void markUnhealthy_shouldExcludeFromSelection() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.markUnhealthy("slave_1");

        String result = loadBalancer.select();
        assertThat(result).isEqualTo("slave_2");
    }

    @Test
    @DisplayName("标记健康的数据源")
    void markHealthy_shouldIncludeInSelection() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.markUnhealthy("slave_1");
        loadBalancer.markHealthy("slave_1");

        assertThat(loadBalancer.getUnhealthyDataSources()).doesNotContain("slave_1");
    }

    @Test
    @DisplayName("使用权重策略")
    void select_weightedStrategy_shouldFollowWeights() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("slave_1", 1);
        weights.put("slave_2", 9);

        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new WeightedStrategy(weights));

        // 多次选择，统计分布
        Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            String selected = loadBalancer.select();
            counts.merge(selected, 1, Integer::sum);
        }

        // slave_2 权重更高，应该被选中更多次
        int slave1Count = counts.getOrDefault("slave_1", 0);
        int slave2Count = counts.getOrDefault("slave_2", 0);
        assertThat(slave2Count).isGreaterThan(slave1Count);
    }

    @Test
    @DisplayName("使用最少连接策略")
    void select_leastConnections_shouldSelectLeastConnected() {
        LeastConnectionsStrategy leastConnStrategy = new LeastConnectionsStrategy();
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                leastConnStrategy);

        // 增加 slave_1 的连接数
        leastConnStrategy.incrementConnection("slave_1");
        leastConnStrategy.incrementConnection("slave_1");

        // 应该选择 slave_2（连接数更少）
        String result = loadBalancer.select();
        assertThat(result).isEqualTo("slave_2");
    }

    @Test
    @DisplayName("设置权重")
    void setWeight_shouldUpdateWeight() {
        WeightedStrategy weightedStrategy = new WeightedStrategy(new HashMap<>());
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                weightedStrategy);

        loadBalancer.setWeight("slave_1", 10);

        assertThat(weightedStrategy.getWeight("slave_1")).isEqualTo(10);
    }

    @Test
    @DisplayName("切换策略")
    void setStrategy_shouldChangeStrategy() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.setStrategy(new LeastConnectionsStrategy());

        assertThat(loadBalancer.getStrategy().getName()).isEqualTo("LEAST_CONNECTIONS");
    }

    @Test
    @DisplayName("手动触发健康检查")
    void triggerHealthCheck_shouldCheckAllDataSources() throws Exception {
        // 重新设置 mock，一个健康一个不健康
        lenient().when(resultSet.next()).thenReturn(true, true);

        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.triggerHealthCheck();

        // 两个数据源都应该健康
        assertThat(loadBalancer.getUnhealthyDataSources()).isEmpty();
    }

    @Test
    @DisplayName("获取健康的候选数据源")
    void getHealthyCandidates_shouldReturnHealthyOnly() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        loadBalancer.markUnhealthy("slave_1");

        List<String> healthy = loadBalancer.getHealthyCandidates();
        assertThat(healthy).containsExactly("slave_2");
    }

    @Test
    @DisplayName("健康检查启动和停止")
    void healthCheck_startAndStop() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy(),
                1000);

        loadBalancer.startHealthCheck();
        assertThat(loadBalancer.isHealthCheckEnabled()).isTrue();

        loadBalancer.stopHealthCheck();
        assertThat(loadBalancer.isHealthCheckEnabled()).isFalse();
    }

    @Test
    @DisplayName("获取主数据源名称")
    void getPrimaryDataSource_shouldReturnPrimary() {
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                new RoundRobinStrategy());

        assertThat(loadBalancer.getPrimaryDataSource()).isEqualTo("master");
    }

    @Test
    @DisplayName("释放连接（最少连接策略）")
    void release_shouldDecrementConnection() {
        LeastConnectionsStrategy leastConnStrategy = new LeastConnectionsStrategy();
        loadBalancer = new ReadDataSourceLoadBalancer(
                dataSourceMap,
                "master",
                List.of("slave_1", "slave_2"),
                leastConnStrategy);

        // 选择会增加连接计数
        loadBalancer.select();
        assertThat(leastConnStrategy.getConnectionCount("slave_1")).isEqualTo(1);

        // 释放会减少连接计数
        loadBalancer.release("slave_1");
        assertThat(leastConnStrategy.getConnectionCount("slave_1")).isEqualTo(0);
    }
}