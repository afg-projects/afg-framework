package io.github.afgprojects.framework.core.datasource.lb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RoundRobinStrategy 单元测试
 */
@DisplayName("RoundRobinStrategy 测试")
class RoundRobinStrategyTest {

    private RoundRobinStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinStrategy();
    }

    @Test
    @DisplayName("单数据源应始终返回同一个")
    void select_singleDataSource_shouldReturnSame() {
        List<String> candidates = List.of("slave_1");

        String result = strategy.select(candidates);

        assertThat(result).isEqualTo("slave_1");
    }

    @Test
    @DisplayName("多数据源应轮询返回")
    void select_multipleDataSources_shouldRoundRobin() {
        List<String> candidates = List.of("slave_1", "slave_2", "slave_3");

        // 连续调用3次
        String first = strategy.select(candidates);
        String second = strategy.select(candidates);
        String third = strategy.select(candidates);

        // 验证轮询顺序
        assertThat(first).isEqualTo("slave_1");
        assertThat(second).isEqualTo("slave_2");
        assertThat(third).isEqualTo("slave_3");
    }

    @Test
    @DisplayName("轮询应循环")
    void select_shouldCycle() {
        List<String> candidates = List.of("slave_1", "slave_2");

        // 调用3次，应循环
        String first = strategy.select(candidates);
        String second = strategy.select(candidates);
        String third = strategy.select(candidates);

        assertThat(first).isEqualTo("slave_1");
        assertThat(second).isEqualTo("slave_2");
        assertThat(third).isEqualTo("slave_1");
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
    @DisplayName("策略名称应为 ROUND_ROBIN")
    void getName_shouldReturnCorrectName() {
        assertThat(strategy.getName()).isEqualTo("ROUND_ROBIN");
    }

    @Test
    @DisplayName("线程安全测试")
    void select_concurrentAccess_shouldBeThreadSafe() throws InterruptedException {
        List<String> candidates = List.of("slave_1", "slave_2", "slave_3");
        int threadCount = 10;
        int iterationsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    String result = strategy.select(candidates);
                    assertThat(candidates).contains(result);
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }
}