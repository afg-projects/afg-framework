package io.github.afgprojects.framework.core.datasource.lb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * WeightedStrategy 单元测试
 */
@DisplayName("WeightedStrategy 测试")
class WeightedStrategyTest {

    private WeightedStrategy strategy;

    @BeforeEach
    void setUp() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("slave_1", 1);
        weights.put("slave_2", 2);
        weights.put("slave_3", 3);
        strategy = new WeightedStrategy(weights);
    }

    @Test
    @DisplayName("单数据源应始终返回同一个")
    void select_singleDataSource_shouldReturnSame() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("slave_1", 5);
        WeightedStrategy singleStrategy = new WeightedStrategy(weights);
        List<String> candidates = List.of("slave_1");

        String result = singleStrategy.select(candidates);

        assertThat(result).isEqualTo("slave_1");
    }

    @Test
    @DisplayName("空权重配置应退化为轮询")
    void select_emptyWeights_shouldFallbackToRoundRobin() {
        WeightedStrategy emptyStrategy = new WeightedStrategy(null);
        List<String> candidates = List.of("slave_1", "slave_2");

        String first = emptyStrategy.select(candidates);
        String second = emptyStrategy.select(candidates);

        assertThat(first).isEqualTo("slave_1");
        assertThat(second).isEqualTo("slave_2");
    }

    @Test
    @DisplayName("权重选择应按权重分布")
    void select_weightedDistribution_shouldFollowWeights() {
        Map<String, Integer> weights = new HashMap<>();
        weights.put("slave_1", 1);
        weights.put("slave_2", 9);
        WeightedStrategy weightedStrategy = new WeightedStrategy(weights);
        List<String> candidates = List.of("slave_1", "slave_2");

        Map<String, Integer> counts = new HashMap<>();
        int iterations = 10000;

        for (int i = 0; i < iterations; i++) {
            String selected = weightedStrategy.select(candidates);
            counts.merge(selected, 1, Integer::sum);
        }

        // slave_2 权重是 slave_1 的9倍，所以选中次数应该约为9倍
        int slave1Count = counts.getOrDefault("slave_1", 0);
        int slave2Count = counts.getOrDefault("slave_2", 0);

        // 允许一定误差范围 (±10%)
        double ratio = (double) slave2Count / slave1Count;
        assertThat(ratio).isBetween(7.0, 11.0);
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
    @DisplayName("策略名称应为 WEIGHTED")
    void getName_shouldReturnCorrectName() {
        assertThat(strategy.getName()).isEqualTo("WEIGHTED");
    }

    @Test
    @DisplayName("动态设置权重")
    void setWeight_shouldUpdateWeight() {
        strategy.setWeight("slave_1", 10);

        assertThat(strategy.getWeight("slave_1")).isEqualTo(10);
    }

    @Test
    @DisplayName("移除权重")
    void removeWeight_shouldRemoveWeight() {
        strategy.removeWeight("slave_1");

        assertThat(strategy.getWeight("slave_1")).isEqualTo(1); // 默认权重为1
    }

    @Test
    @DisplayName("候选数据源没有权重配置时应退化为轮询")
    void select_noWeightedCandidates_shouldFallbackToRoundRobin() {
        WeightedStrategy weightedStrategy = new WeightedStrategy(Map.of("other", 1));
        List<String> candidates = List.of("slave_1", "slave_2");

        // 不应该抛出异常，而是退化为轮询
        String result = weightedStrategy.select(candidates);
        assertThat(candidates).contains(result);
    }
}