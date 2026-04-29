package io.github.afgprojects.framework.core.datasource.lb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * LeastConnectionsStrategy 单元测试
 */
@DisplayName("LeastConnectionsStrategy 测试")
class LeastConnectionsStrategyTest {

    private LeastConnectionsStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new LeastConnectionsStrategy();
    }

    @Test
    @DisplayName("单数据源应始终返回同一个")
    void select_singleDataSource_shouldReturnSame() {
        List<String> candidates = List.of("slave_1");

        String result = strategy.select(candidates);

        assertThat(result).isEqualTo("slave_1");
    }

    @Test
    @DisplayName("应选择连接数最少的数据源")
    void select_shouldSelectLeastConnections() {
        List<String> candidates = List.of("slave_1", "slave_2", "slave_3");

        // 设置连接数
        strategy.incrementConnection("slave_1");
        strategy.incrementConnection("slave_1");
        strategy.incrementConnection("slave_2");

        // slave_3 连接数为0，应该被选中
        String result = strategy.select(candidates);
        assertThat(result).isEqualTo("slave_3");
    }

    @Test
    @DisplayName("连接数相同时应选择第一个")
    void select_sameConnections_shouldSelectFirst() {
        List<String> candidates = List.of("slave_1", "slave_2", "slave_3");

        // 所有数据源连接数都为0
        String result = strategy.select(candidates);
        assertThat(result).isEqualTo("slave_1");
    }

    @Test
    @DisplayName("增加和减少连接计数")
    void connectionCount_shouldIncrementAndDecrement() {
        strategy.incrementConnection("slave_1");
        strategy.incrementConnection("slave_1");
        assertThat(strategy.getConnectionCount("slave_1")).isEqualTo(2);

        strategy.decrementConnection("slave_1");
        assertThat(strategy.getConnectionCount("slave_1")).isEqualTo(1);
    }

    @Test
    @DisplayName("连接数不能为负")
    void decrementConnection_shouldNotGoNegative() {
        strategy.decrementConnection("slave_1");
        assertThat(strategy.getConnectionCount("slave_1")).isEqualTo(0);
    }

    @Test
    @DisplayName("重置连接计数")
    void resetConnectionCount_shouldReset() {
        strategy.incrementConnection("slave_1");
        strategy.incrementConnection("slave_1");
        strategy.resetConnectionCount("slave_1");

        assertThat(strategy.getConnectionCount("slave_1")).isEqualTo(0);
    }

    @Test
    @DisplayName("空列表应抛出异常")
    void select_emptyList_shouldThrowException() {
        List<String> candidates = List.of();

        assertThatThrownBy(() -> strategy.select(candidates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("策略名称应为 LEAST_CONNECTIONS")
    void getName_shouldReturnCorrectName() {
        assertThat(strategy.getName()).isEqualTo("LEAST_CONNECTIONS");
    }

    @Test
    @DisplayName("移除数据源")
    void removeDataSource_shouldRemove() {
        strategy.incrementConnection("slave_1");
        strategy.removeDataSource("slave_1");

        assertThat(strategy.getConnectionCount("slave_1")).isEqualTo(0);
    }

    @Test
    @DisplayName("获取所有连接计数")
    void getConnectionCounts_shouldReturnAll() {
        strategy.incrementConnection("slave_1");
        strategy.incrementConnection("slave_2");
        strategy.incrementConnection("slave_2");

        var counts = strategy.getConnectionCounts();

        assertThat(counts.get("slave_1")).isEqualTo(1);
        assertThat(counts.get("slave_2")).isEqualTo(2);
    }

    @Test
    @DisplayName("动态连接场景测试")
    void select_dynamicConnections_shouldAdapt() {
        List<String> candidates = List.of("slave_1", "slave_2");

        // 初始状态选择 slave_1
        String first = strategy.select(candidates);
        assertThat(first).isEqualTo("slave_1");

        // 增加 slave_1 的连接
        strategy.incrementConnection("slave_1");

        // 现在应该选择 slave_2（连接数更少）
        String second = strategy.select(candidates);
        assertThat(second).isEqualTo("slave_2");

        // 增加 slave_2 的连接
        strategy.incrementConnection("slave_2");

        // 现在两者连接数相同，选择第一个
        String third = strategy.select(candidates);
        assertThat(third).isEqualTo("slave_1");
    }
}